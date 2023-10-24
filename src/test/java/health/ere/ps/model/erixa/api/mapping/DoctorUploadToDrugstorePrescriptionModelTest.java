package health.ere.ps.model.erixa.api.mapping;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoctorUploadToDrugstorePrescriptionModelTest {

    static PrescriptionData prescriptionData = new PrescriptionData();
    static DoctorUploadToDrugstorePrescriptionModel doctorUploadToDrugstorePrescriptionModel = new DoctorUploadToDrugstorePrescriptionModel();

    @BeforeAll
    public static void init() throws FileNotFoundException, ParseException
    {
        doctorUploadToDrugstorePrescriptionModel.setBase64File("base64File");
        doctorUploadToDrugstorePrescriptionModel.setDrugstoreEmailAddress("email");
        doctorUploadToDrugstorePrescriptionModel.setDrugstoreSourceType(1);
        doctorUploadToDrugstorePrescriptionModel.setFileName("filename");
        doctorUploadToDrugstorePrescriptionModel.setFileSize(1000);
        doctorUploadToDrugstorePrescriptionModel.setFileType("excel");
        doctorUploadToDrugstorePrescriptionModel.setPrescriptionData(prescriptionData);
    }

    @Test
    public void testDoctorUploadToDrugstorePrescriptionModel() {
        assertEquals("base64File", doctorUploadToDrugstorePrescriptionModel.getBase64File());
        assertEquals("email", doctorUploadToDrugstorePrescriptionModel.getDrugstoreEmailAddress());
        assertEquals(1, doctorUploadToDrugstorePrescriptionModel.getDrugstoreSourceType());
        assertEquals("filename", doctorUploadToDrugstorePrescriptionModel.getFileName());
        assertEquals(1000, doctorUploadToDrugstorePrescriptionModel.getFileSize());
        assertEquals("excel", doctorUploadToDrugstorePrescriptionModel.getFileType());
        assertEquals(prescriptionData, doctorUploadToDrugstorePrescriptionModel.getPrescriptionData());
    }
}
