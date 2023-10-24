package health.ere.ps.websocket;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.ErrorHandlerAdapter;
import ca.uhn.fhir.parser.XmlParser;
import health.ere.ps.config.AppConfig;
import health.ere.ps.event.BundlesEvent;
import health.ere.ps.event.ERezeptWithDocumentsEvent;
import health.ere.ps.model.config.UserConfigurations;
import health.ere.ps.model.gematik.BundleWithAccessCodeOrThrowable;
import health.ere.ps.model.pdf.ERezeptDocument;
import health.ere.ps.service.config.UserConfigurationService;
import health.ere.ps.validation.fhir.bundle.PrescriptionBundleValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;

import javax.enterprise.event.Event;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.SendHandler;
import javax.websocket.Session;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebsocketTest {

  @Test
  void testOnMessageWithValidSignAndUploadBundles() throws IOException {
    Websocket websocket = new Websocket();
    websocket.signAndUploadBundlesEvent = mock(Event.class);
    websocket.prescriptionBundleValidator = mock(PrescriptionBundleValidator.class);

    String signAndUploadBundles = new String(getClass()
        .getResourceAsStream("/websocket-messages/SignAndUploadBundles.json")
        .readAllBytes(), StandardCharsets.UTF_8);

    JsonObject mockedBundlesValidationResult =
        Json.createObjectBuilder()
            .add("type", "BundlesValidationResult")
            .add("payload", Json.createArrayBuilder().add(Json.createObjectBuilder().add("valid", true).build()))
            .add("replyToMessageId", "")
            .build();

    when(websocket.prescriptionBundleValidator.bundlesValidationResult(any())).thenReturn(mockedBundlesValidationResult);

    websocket.onMessage(signAndUploadBundles, null);
    verify(websocket.signAndUploadBundlesEvent, times(1)).fireAsync(any());
  }

  @Test
  void testOnMessageWithInvalidSignAndUploadBundles() throws IOException {
    Websocket websocket = new Websocket();

    websocket.signAndUploadBundlesEvent = mock(Event.class);
    websocket.prescriptionBundleValidator = mock(PrescriptionBundleValidator.class);
    Session mockedSession = mock(Session.class);
    Async mockedAsync = mock(Async.class);

    String signAndUploadBundles = new String(getClass()
        .getResourceAsStream("/websocket-messages/SignAndUploadBundles.json")
        .readAllBytes(), StandardCharsets.UTF_8);

    JsonObject mockedBundlesValidationResult =
        Json.createObjectBuilder()
            .add("type", "BundlesValidationResult")
            .add("payload", Json.createArrayBuilder().add(Json.createObjectBuilder().add("valid", false).build()))
            .add("replyToMessageId", "123")
            .build();

    when(websocket.prescriptionBundleValidator.bundlesValidationResult(any())).thenReturn(mockedBundlesValidationResult);
    when(mockedSession.getAsyncRemote()).thenReturn(mockedAsync);

    websocket.onMessage(signAndUploadBundles, mockedSession);

    ArgumentCaptor<String> exceptionMessageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SendHandler> sendHandlerCaptor = ArgumentCaptor.forClass(SendHandler.class);

    verify(mockedAsync).sendObject(exceptionMessageCaptor.capture(), sendHandlerCaptor.capture());
    String exception = exceptionMessageCaptor.getValue();

    javax.json.JsonObject exceptionObject = Json.createReader(new StringReader(exception)).readObject();

    assertEquals(mockedBundlesValidationResult, exceptionObject);
    assertEquals("BundlesValidationResult", exceptionObject.getString("type"));
    assertEquals("123", exceptionObject.getString("replyToMessageId"));
  }

  @Test
  void testOnMessageWithRequestSettings() throws IOException {
    Websocket websocket = new Websocket();
    websocket.userConfigurationService = mock(UserConfigurationService.class);
    Session mockedSession = mock(Session.class);
    Async mockedAsync = mock(Async.class);

    String requestSettings = new String(getClass()
        .getResourceAsStream("/websocket-messages/RequestSettings.json")
        .readAllBytes(), StandardCharsets.UTF_8);

    UserConfigurations userConfigurations = new UserConfigurations();
    userConfigurations.setUserId("123");
    userConfigurations.setVersion("2");

    when(websocket.userConfigurationService.getConfig()).thenReturn(userConfigurations);
    when(mockedSession.getAsyncRemote()).thenReturn(mockedAsync);

    websocket.onMessage(requestSettings, mockedSession);

    ArgumentCaptor<String> exceptionMessageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SendHandler> sendHandlerCaptor = ArgumentCaptor.forClass(SendHandler.class);

    verify(mockedAsync).sendObject(exceptionMessageCaptor.capture(), sendHandlerCaptor.capture());
    String exception = exceptionMessageCaptor.getValue();

    javax.json.JsonObject exceptionObject = Json.createReader(new StringReader(exception)).readObject();

    JsonObject payload = exceptionObject.getJsonObject("payload");

    assertEquals("Settings", exceptionObject.getString("type"));
    assertEquals("123", payload.getString("connector.user-id"));
    assertEquals("2", payload.getString("connector.version"));
  }

  @Test
  void testOnMessageWithValidateBundles() throws IOException {
    Websocket websocket = new Websocket();

    websocket.signAndUploadBundlesEvent = mock(Event.class);
    websocket.prescriptionBundleValidator = mock(PrescriptionBundleValidator.class);
    Session mockedSession = mock(Session.class);
    Async mockedAsync = mock(Async.class);

    String signAndUploadBundles = new String(getClass()
        .getResourceAsStream("/websocket-messages/SignAndUploadBundles.json")
        .readAllBytes(), StandardCharsets.UTF_8)
        .replace("SignAndUploadBundles", "ValidateBundles");

    JsonObject mockedBundlesValidationResult =
        Json.createObjectBuilder()
            .add("type", "BundlesValidationResult")
            .add("payload", Json.createArrayBuilder().add(Json.createObjectBuilder().add("valid", false).build()))
            .add("replyToMessageId", "123")
            .build();

    when(websocket.prescriptionBundleValidator.bundlesValidationResult(any())).thenReturn(mockedBundlesValidationResult);
    when(mockedSession.getAsyncRemote()).thenReturn(mockedAsync);

    websocket.onMessage(signAndUploadBundles, mockedSession);

    ArgumentCaptor<String> exceptionMessageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SendHandler> sendHandlerCaptor = ArgumentCaptor.forClass(SendHandler.class);

    verify(mockedAsync).sendObject(exceptionMessageCaptor.capture(), sendHandlerCaptor.capture());
    String exception = exceptionMessageCaptor.getValue();

    javax.json.JsonObject exceptionObject = Json.createReader(new StringReader(exception)).readObject();

    assertEquals(mockedBundlesValidationResult, exceptionObject);
    assertEquals("BundlesValidationResult", exceptionObject.getString("type"));
    assertEquals("123", exceptionObject.getString("replyToMessageId"));
  }

  @Test
  void testOnMessageWithXMLBundle() throws IOException {
    Websocket websocket = mock(Websocket.class);
    websocket.signAndUploadBundlesEvent = mock(Event.class);
    websocket.appConfig = mock(AppConfig.class);

    String xmlBundle = new String(getClass()
        .getResourceAsStream("/websocket-messages/XmlBundlesExample.json")
        .readAllBytes(), StandardCharsets.UTF_8);

    doNothing().when(websocket).assureChromeIsOpen();
    doCallRealMethod().when(websocket).onMessage(any(), any());
    doCallRealMethod().when(websocket).onFhirBundle(any());
    when(websocket.appConfig.getXmlBundleDirectProcess()).thenReturn(true);

    websocket.onMessage(xmlBundle, null);
    verify(websocket.signAndUploadBundlesEvent, times(1)).fireAsync(any());
  }

  @Test
  void testOnMessageWithAllKBVExamples() throws IOException, IllegalAccessException {
    Websocket websocket = mock(Websocket.class);
    FhirContext ctx = mock(FhirContext.class);
    XmlParser parser = mock(XmlParser.class);

    Session mockedSession = mock(Session.class);
    Async mockedAsync = mock(Async.class);

    // reflections on FhirContext
    Field field = ReflectionUtils
        .findFields(Websocket.class, f -> f.getName().equals("ctx"),
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
        .get(0);
    field.setAccessible(true);
    field.set(websocket, ctx);

    // mocking return objects
    when(mockedSession.getAsyncRemote()).thenReturn(mockedAsync);

    doNothing().when(websocket).assureChromeIsOpen();
    doNothing().when(websocket).onFhirBundle(any());
    doCallRealMethod().when(websocket).onMessage(any(), any());
    doCallRealMethod().when(websocket).sendAllKBVExamples(any(), any());

    doReturn(parser).when(ctx).newXmlParser();
    doReturn(new Bundle()).when(parser).parseResource(any(), (String) any());

    // testing and checking that everything is okay under AllKBVExamples websocket message
    String allKbvExamplesBundleFirst = "{\"type\": \"AllKBVExamples\", \"folder\": \"src/test/resources/examples-kbv-fhir-erp-v1-0-2\"}";
    websocket.onMessage(allKbvExamplesBundleFirst, null);

    String allKbvExamplesBundleSecond = "{\"type\": \"AllKBVExamples\", \"folder\": \"src/test/resources/kbv-zip\"}";
    websocket.onMessage(allKbvExamplesBundleSecond, mockedSession);
  }

  @Test
  void testOnMessageWithEvents() throws IOException {
    Websocket websocket = new Websocket();
    websocket.abortTasksEvent = mock(Event.class);
    websocket.erixaEvent = mock(Event.class);
    websocket.deactivateComfortSignatureEvent = mock(Event.class);
    websocket.activateComfortSignatureEvent = mock(Event.class);
    websocket.getSignatureModeEvent = mock(Event.class);
    websocket.getCardsEvent = mock(Event.class);
    websocket.changePinEvent = mock(Event.class);
    websocket.verifyPinEvent = mock(Event.class);
    websocket.unblockPinEvent = mock(Event.class);
    websocket.getPinStatusEvent = mock(Event.class);
    websocket.prefillBundleEvent = mock(Event.class);
    websocket.vZDSearchEvent = mock(Event.class);
    websocket.requestStatusEvent = mock(Event.class);

    String abortTaskEvent = "{\"type\": \"AbortTasks\"}";

    websocket.onMessage(abortTaskEvent, null);
    verify(websocket.abortTasksEvent, times(1)).fireAsync(any());

    String erixaEvent = "{\"type\": \"ErixaEvent\", \"payload\": {\"payloadInside\": \"ErixaEvent\"}, \"processType\": \"ErixaEvent\"}";

    websocket.onMessage(erixaEvent, null);
    verify(websocket.erixaEvent, times(1)).fireAsync(any());

    String deactivateComfortSignatureEvent = "{\"type\": \"DeactivateComfortSignature\"}";

    websocket.onMessage(deactivateComfortSignatureEvent, null);
    verify(websocket.deactivateComfortSignatureEvent, times(1)).fireAsync(any());

    String activateComfortSignatureEvent = "{\"type\": \"ActivateComfortSignature\"}";

    websocket.onMessage(activateComfortSignatureEvent, null);
    verify(websocket.activateComfortSignatureEvent, times(1)).fireAsync(any());

    String getSignatureModeEvent = "{\"type\": \"GetSignatureMode\"}";

    websocket.onMessage(getSignatureModeEvent, null);
    verify(websocket.getSignatureModeEvent, times(1)).fireAsync(any());

    String getCardsEvent = "{\"type\": \"GetCards\"}";

    websocket.onMessage(getCardsEvent, null);
    verify(websocket.getCardsEvent, times(1)).fireAsync(any());

    String changePinEvent = "{\"type\": \"ChangePin\", \"payload\": {\"cardHandle\": \"ChangePing\", \"pinType\": \"ChangePin\"}}";

    websocket.onMessage(changePinEvent, null);
    verify(websocket.changePinEvent, times(1)).fireAsync(any());

    String verifyPinEvent = "{\"type\": \"VerifyPin\", \"payload\": {\"cardHandle\": \"VerifyPin\", \"pinType\": \"VerifyPin\"}}";

    websocket.onMessage(verifyPinEvent, null);
    verify(websocket.verifyPinEvent, times(1)).fireAsync(any());

    String unblockPinEvent = "{\"type\": \"UnblockPin\", \"payload\": {\"cardHandle\": \"UnblockPin\", \"pinType\": \"UnblockPin\", \"setNewPin\": false}}";

    websocket.onMessage(unblockPinEvent, null);
    verify(websocket.unblockPinEvent, times(1)).fireAsync(any());

    String getPinStatusEvent = "{\"type\": \"GetPinStatus\", \"payload\": {\"cardHandle\": \"GetPinStatus\", \"pinType\": \"GetPinStatus\"}}";

    websocket.onMessage(getPinStatusEvent, null);
    verify(websocket.getPinStatusEvent, times(1)).fireAsync(any());

    String prefillBundleEvent = "{\"type\": \"PrefillBundle\"}";

    websocket.onMessage(prefillBundleEvent, null);
    verify(websocket.prefillBundleEvent, times(1)).fireAsync(any());

    String vZDSearchEvent = "{\"type\": \"VZDSearch\", \"payload\": {\"cardHandle\": \"VZDSearch\", \"pinType\": \"VZDSearch\"}, \"search\": \"VZDSearch\"}";

    websocket.onMessage(vZDSearchEvent, null);
    verify(websocket.vZDSearchEvent, times(1)).fireAsync(any());

    String requestStatusEvent = "{\"type\": \"RequestStatus\"}";

    websocket.onMessage(requestStatusEvent, null);
    verify(websocket.requestStatusEvent, times(1)).fireAsync(any());
  }

  // Passing but also generating LogManager errors since the introduction of the validation checks
  // in the websocket.
  @Test
  void testGetJsonEventFor() throws IOException {
    Websocket websocket = new Websocket();
    List<BundleWithAccessCodeOrThrowable> list = new ArrayList<>();
    Bundle bundle = (Bundle) FhirContext.forR4().newXmlParser().parseResource(
				getClass().getResourceAsStream("/examples_erezept/Erezept_template_2.xml"));
    list.add(new BundleWithAccessCodeOrThrowable(bundle, "MOCK_ACCESS_CODE"));
    ERezeptDocument eRezeptDocument = new ERezeptDocument(list, Files.readAllBytes(Paths.get("src/test/resources/document-service/0428d416-149e-48a4-977c-394887b3d85c.pdf")));
      String json = websocket.generateJson(new ERezeptWithDocumentsEvent(List.of(eRezeptDocument)));

    Files.writeString(Paths.get("src/test/resources/websocket-messages/ERezeptDocuments.json"), json);
  }
  @Test
  void testOnMessageNull() {
    Websocket websocket = new Websocket();
    websocket.onMessage(null, null);
  }
  @Test
  void testOnMessageInvalidJson() {
    Websocket websocket = new Websocket();
    websocket.onMessage("asdasdsad", null);
  }

  @Test
  void testOnMessageWithPublish() throws IOException {
    Websocket websocket = new Websocket();
    String publishMessage = "{\"type\": \"Publish\", \"payload\": {\"cardHandle\": \"Publish\", \"pinType\": \"Publish\"}, \"search\": \"Publish\"}";
    websocket.onMessage(publishMessage, null);
  }

  @Test
  void testOnMessageInvalidJsonWithReplyTo() {
    Websocket websocket = new Websocket();
    Session mockedSession = mock(Session.class);

    Async mockedAsync = mock(Async.class);

    when(mockedSession.getAsyncRemote()).thenReturn(mockedAsync);

    websocket.onMessage("asdasdsad", mockedSession);

    ArgumentCaptor<String> exceptionMessageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SendHandler> sendHandlerCaptor = ArgumentCaptor.forClass(SendHandler.class);

    verify(mockedAsync).sendObject(exceptionMessageCaptor.capture(), sendHandlerCaptor.capture());
    String exception = exceptionMessageCaptor.getValue();

    javax.json.JsonObject exceptionObject = Json.createReader(new StringReader(exception)).readObject();

    assertEquals("Exception", exceptionObject.getString("type"));

  }
}
