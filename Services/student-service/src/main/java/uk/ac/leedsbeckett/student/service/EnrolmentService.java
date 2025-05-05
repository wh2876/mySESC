package uk.ac.leedsbeckett.student.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.leedsbeckett.student.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
public class EnrolmentService extends Service{
    private final StudentRepository studentRepository;
    private final FinanceIntegrationService financeIntegrationService;

    public EnrolmentService(StudentRepository studentRepository, FinanceIntegrationService financeIntegrationService) {
        this.studentRepository = studentRepository;
        this.financeIntegrationService = financeIntegrationService;
    }

    //creates an invoice, all invoices are set to be due in 1 year from the system's time, not sure if this is the best
    // some may have later/earlier due dates and theres potential the system's time is tampered with and therefore due dates are wrong (unlikely)
    Invoice buildInvoice(Student student, Course course){
        Invoice invoice = new Invoice();
        invoice.setAmount(course.getFee());
        invoice.setDueDate(LocalDate.now().plusYears(1));
        invoice.setType(Invoice.Type.TUITION_FEES);
        invoice.setAccount(
                financeIntegrationService.getAccount(
                        student.getExternalStudentId()));
        return invoice;
    }

    //this is the most recent and the best example of response handling I've implemented, if I had the time I'd go back and change my response returning handling to be more specific to the problems
    
    public ResponseEntity<?> enrollStudentInCourse(Student student, Course course) {
        //attempt to enrol, if failed return before invoice created
        if(!student.enrolInCourse(course)) {return ResponseEntity.badRequest().body("Student already enrolled in course");}
        //create course invoice
        ResponseEntity<?> r =  financeIntegrationService.createInvoiceModel(buildInvoice(student, course));
        if(r.getStatusCode() == HttpStatus.OK) {
            studentRepository.save(student);
            return ResponseEntity.ok().body("Successfully enrolled in course. Invoice created.");
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Could not enroll at this time.");
    }
    public ResponseEntity<?> disenrollStudentFromCourse(Student student, Course course) {

        if(!student.removeCourse(course)) {return ResponseEntity.badRequest().body("Student already not enrolled in course");}

        //CANCEL THE TUITION FEE FOR THIS COURSE // SHOULD ONLY BE DONE IF SWAPPING COURSES
        //IT MAY BE THAT UNENROLLING SHOULD NEVER CANCEL THE FEES
        List<Invoice> courseInvoices = financeIntegrationService.getAllInvoices();
        for (Invoice invoice : courseInvoices) {
            assert invoice != null;
            if (invoice.getAccount().getStudentId().equals(student.getExternalStudentId())//check invoice is for this studenT
                    && invoice.getType().equals(Invoice.Type.TUITION_FEES)   //check invioce is tuition fee
                    && Objects.equals(invoice.getAmount(), course.getFee())//check invoice costs the course fee //THE MOST UNRELIABLE BIT
                    // if invoice matches type, account, and fee, cancel this invoice
                    && Objects.equals(invoice.getStatus(), Invoice.Status.OUTSTANDING)) {

                ResponseEntity<?> r = financeIntegrationService.cancelInvoiceModel(invoice.getReference());
                if(r.getStatusCode() == HttpStatus.OK) {
                    studentRepository.save(student);
                    return ResponseEntity.ok().body("Successfully unenrolled from course. Invoice cancelled.");
                }
                break;
                //problems may occur if:
                // 1. course has changed fee (invoice will not change with it automatically so it will be missed here)
                // 2. another course this student is enrolled in has the same fee and its invoice was created first (that one will be cancelled instead)
            }
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Could not unenroll at this time..");
    }

}
