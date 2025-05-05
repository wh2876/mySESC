package uk.ac.leedsbeckett.student.service;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import uk.ac.leedsbeckett.student.controller.StudentController;
import uk.ac.leedsbeckett.student.model.*;
import uk.ac.leedsbeckett.student.model.Student;

import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class StudentService extends Service {

    private final StudentRepository studentRepository;
    private final CourseService courseService;
    private final FinanceIntegrationService financeIntegrationService;
    private final LibraryIntegrationService libraryIntegrationService;
    private final EnrolmentService enrolmentService;

    public StudentService(StudentRepository studentRepository,
                          EnrolmentService enrolmentService,
                          CourseService courseService,
                          FinanceIntegrationService financeIntegrationService,
                          LibraryIntegrationService libraryIntegrationService) {
        this.studentRepository = studentRepository;
        this.enrolmentService = enrolmentService;
        this.courseService = courseService;
        this.financeIntegrationService = financeIntegrationService;
        this.libraryIntegrationService = libraryIntegrationService;
    }


//admin / internal methods
    private EntityModel<Student> getStudentEntityModel(Student student){
        return EntityModel.of(student,
                linkTo(methodOn(StudentController.class).getStudentJson(student.getId())).withSelfRel(),
                linkTo(methodOn(StudentController.class).getStudentsJson()).withRel("students"));
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
    public ResponseEntity<CollectionModel<EntityModel<Student>>> getAllStudentsJson(){
        List<Student> studentList = getAllStudents();
        CollectionModel<EntityModel<Student>> col = CollectionModel.of(studentList
                .stream()
                .map(this::getStudentEntityModel)
                .toList());
        return ResponseEntity.ok(CollectionModel.of(col, linkTo(methodOn(StudentController.class)
                .getStudentsJson())
                .withSelfRel()));
    }

    public Student getStudentById(Long id){
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No student found with id " + id));
    }
    public ResponseEntity<?> getStudentByIdJson(Long id){
       try{
           return makeResponseEntity(getStudentEntityModel(getStudentById(id)));
       }catch (Exception e){
           return getProblemResponse(e);
       }

    }

    //id is the id field of Student, this is for:
    // A) creating, id is null, and so its checking for a new studentId
    // B) editing, id indicates that if the studentId is in use then if that studentId belongs to the student being edited its fine
    public boolean isAvailableStudentId(String studentId, Long id){
        if (id==null) return getAllStudents().stream().noneMatch(student -> student.getExternalStudentId().equals(studentId));
        List<Student> stus = getAllStudents().stream().filter(
                s -> s.getExternalStudentId().equals(studentId)).toList();
        //stus should have 1 or 0 items
        return stus.getFirst().getId().equals(id);
    }

    //IMPROVEMENT: should check that finance and library services are available before attempting
    //otherwise student will be created with no library or finance account which means no tuition fees/book permissions
    public Student createNewStudent(Student student){
        try {
            Student addedStudent = studentRepository.save(student);
            try {
                financeIntegrationService.createFinanceAccount(
                        new FinanceAccount(student.getExternalStudentId()));
            }catch (Exception ignored){
                //if fails to make finance account, this problem should be handled whenever finance account is relevant
                //should 1. not allow enrolling and 2. try to create a new finance account whenever student logs in
            }
            try {
                libraryIntegrationService.createLibraryAccount(
                        addedStudent.getExternalStudentId());
            } catch (Exception ignored) {
                //library service may be unavailable or studentId may be in use by a previous student.
                //if id in use i.e. student was deleted but library account not, tell library to delete account
                //library only has create endpoint afaik so this is a problem
                //currently the student gets access to a library accoutn though, jsut a used one
                //they inherit overude and if they return them will get a fine
            }
            return addedStudent;
        }catch (Exception e){
            throw new RuntimeException("Unable to create student with studentId " + student.getExternalStudentId(), e);
        }

    }
    public ResponseEntity<?> createNewStudentJson(Student student){
        try{
            Student addedStudent = createNewStudent(student);
            return makeResponseEntity(getStudentEntityModel(addedStudent));
        }catch (Exception e){
            return getProblemResponse(e);
        }

    }

    public Student editStudent(Long id, Student student){
        if(!isAvailableStudentId(student.getExternalStudentId(),id)) throw new RuntimeException("Student id " + student.getExternalStudentId() + " is not available");
        Student editedStudent = getStudentById(id);
        editedStudent.setForename(student.getForename());
        editedStudent.setSurname(student.getSurname());
        editedStudent.setExternalStudentId(student.getExternalStudentId());
        editedStudent.setCoursesEnrolledIn(student.getCoursesEnrolledIn());
        return studentRepository.save(editedStudent);
    }
    public ResponseEntity<?> editStudentJson(Long id, Student student){
        try {
            Student editedStudent = editStudent(id, student);
            return makeResponseEntity(getStudentEntityModel(editedStudent));
        }catch (Exception e){
            return getProblemResponse(e);
        }
    }

    public boolean deleteStudent(Long id){
        if(!studentRepository.existsById(id)){return false;}
        studentRepository.deleteById(id);
        return true;
    }
    public ResponseEntity<?> deleteStudentJson(Long id){
        try{
            if (!deleteStudent(id)) throw new Exception("No student found with id " + id);
            return ResponseEntity.ok().build();
        }
        catch(Exception e){
            return getProblemResponse(e);
        }
    }

    //returns either a fail or an invoice
    public ResponseEntity<?> enrollStudentJson(Long studentId, Long courseId){
        try {
            Student stu = getStudentById(studentId);
            Course crs = courseService.getCourseById(courseId);
            return enrolmentService.enrollStudentInCourse(stu, crs);
        }catch (Exception e){
            return getProblemResponse(e);
        }
    }
    //returns either a fail or a "yeah ok done"
    public ResponseEntity<?> disenrollStudentJson(Long studentId, Long courseId){
        try {
            Student stu = getStudentById(studentId);
            return enrolmentService.disenrollStudentFromCourse(stu, courseService.getCourseById(courseId));
        }catch (Exception e){
            return getProblemResponse(e);
        }
    }


//PORTAL METHODS
    //for convenience
    private Student getSessionUser(Model model){
        return (Student) model.getAttribute("student");
    }
//● Register/Log in - create a portal user and log in.
//CREATING PORTAL
    //used to do other stuff but i moved it around so calls to showPortal could be replaced with "portal"
    //returns "portal" to say "load portal.html"
    public String showPortal(Model model) {
        return "portal";
    }

//REGISTERING
    //turns String "c056" into int 560000
    public int getNumFromStudentId(String id){
        int newNum = 0;
        int numLength = id.length()-1; //should be 7
        // but in case of anomalies (i.e. c123) padding occurs (i.e. c123 -> c1230000)
        for (int i = 1; i <= numLength; ++i) {
            int digit = (int)id.charAt(i)-48;
            newNum += (int) (digit * Math.pow(10, 7 - i));
        }
        return newNum;
    }
    //turns int 560000 into id String "c0560000"
    public String makeIdFromNum(int num){
        StringBuilder str = new StringBuilder("c");
        for (int i = 1; i < 8; ++i){
            int digit = (num / (int) Math.pow(10, 7 - i)) % 10;
            str.append(digit);
        }
            //truncate num
            /* i.e. for  c0034206
            as int it is 34206
            "c" -> "c"
            i = 1 -> 34206/10^(7-1) = 34206/1000000%10 = 0%10 = 0 -> c0
            i = 2 -> 34206/10^(7-2) = 34206/100000%10 = 0%10 = 0 -> c00
            i = 3 -> 34206/10^(7-3) = 34206/10000%10 = 3%10 = 3 -> c003
            i = 4 -> 34206/10^(7-4) = 34206/1000%10 = 34%10 = 4 -> c0034
            i = 5 -> 34206/10^(7-5) = 34206/100%10 = 342%10 = 2 -> c00342
            i = 6 -> 34206/10^(7-6) = 34206/10%10 = 3420%10 = 0 -> c003420
            i = 7 -> 34206/10^(7-7) = 34206/1%10 = 34206%10 = 6 -> c0034206
             */
        return str.toString();
    }
    //does as it says. generates a currently unused studentID (starting from the id given)
    public String generateNewStudentId(String startId){

        //studentId XXXXXXX number to try
        int newNum;
        if(startId==null) newNum = 0;
        else newNum = getNumFromStudentId(startId);

        while(true){
            //check if id is in use and +1 to num for next id
            String newId = makeIdFromNum(newNum++);
            if(isAvailableStudentId(newId,null))
                return newId;
        }
    }
    public String registerPortalUser(String firstName, String lastName, BindingResult bindingResult, Model model) {

        //if binding has errors it means register info is invalid
        if (bindingResult.hasErrors()){
            //signInInfo gets deleted since it isnt part of the form and therefore the model
            // so re-add it to allow the html to load fine
            model.addAttribute("signInInfo",new SignInInfo());
            //refresh page (will now display input problems thanks to validation and the alert div)
            return "student";
        }

        //build new student
        //realistically should include some further anti student duplication verification here, i.e. email, address, finance info, passport, etc.
        Student stu = new Student();
        stu.setExternalStudentId(
                generateNewStudentId(
                        getAllStudents().getLast().getExternalStudentId())); //generates new student Id as last id +1
        stu.setForename(firstName);
        stu.setSurname(lastName);

        //officially register student and log them in
        stu = createNewStudent(stu);
        model.addAttribute("student",stu);
        return attemptLogIn(stu.getExternalStudentId(),bindingResult, model);
    }

//LOGGING IN/OUT
    public String attemptLogIn(String signInID, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            //if signInInfo invalid
            //reload page elements without refreshing data
            model.addAttribute("registerInfo",new RegisterInfo()); //re-add cos it gets lost cost it wasnt part of the form
            return "student";
        }

        //get other student with this id
        // (i.e. check if there is an account to log in to)
        //could be done with anyMatch but this way we can use the student found if there is one
        //theres probably a List method for this but i didnt find it while i was making this and it works fine
        List<Student> stus = getAllStudents().stream().filter(
                s -> s.getExternalStudentId().equals(signInID)).toList();
        //stus should have 1 or 0 items
        if(stus.isEmpty()){
            bindingResult.rejectValue("externalStudentId", "externalStudentId.notfound", "Student not found.");
            model.addAttribute("registerInfo",new RegisterInfo()); //re-add cos it gets lost cost it wasnt part of the form
            return "student";
        }
        //if valid credentials, log in
        model.addAttribute("student",stus.getFirst());
        return showAccount(model);
    }
    public String logOut(Model model) {
        //reset session user info
        model.addAttribute("student",new Student());
        return "redirect:/portal/account";
    }

//  ● View Courses - view all the courses offered.
    public String showAvailableCourses(Model model) {
        //refresh session user info on reloading course page to accurately show which courses are enrolled in
        Student ss = getSessionUser(model);
        if(ss.dataFilled()) model.addAttribute("student",getStudentById(getSessionUser(model).getId()));

        //model.addAttribute objects are used in the html files in resources/templates
        model.addAttribute("financeConnected",false);
        if(financeIntegrationService.serviceOnline()){
            model.addAttribute("financeConnected",true);
        }
        model.addAttribute("courses", courseService.getAllCourses());
        return "courses";
    }

//  ● Enrol in Course - enrol in course. If this is your first enrolment, a student account is
//    created at this point.
    public String enrolInCourse(Long courseId, Model model) {
        //if student is not logged in they cannot enroll. bring them to the sign in/register page
        Student stu = getSessionUser(model);
        if (stu == null || !stu.dataFilled()) return "redirect:/portal/account";


        //if student is enrolled, they are trying to unenroll, so do that
        if (stu.isInCourse(courseService.getCourseById(courseId))) {
            ResponseEntity<?> r = disenrollStudentJson(stu.getId(), courseId);
            //should notify the user on outcome of attempt
            if (r.getStatusCode() == HttpStatus.OK) return showAvailableCourses(model);
                //if invoice couldnt be cancelled
            else throw new RuntimeException(r.getStatusCode().toString());
        }//else if not enrolled, enroll them
        else {
            ResponseEntity<?> r = enrollStudentJson(stu.getId(), courseId); //calls json so if throws error the json method handles them to prevent further problems
            if (r.getStatusCode() == HttpStatus.OK) return showAvailableCourses(model);
                //if invoice couldnt be cancelled
            else throw new RuntimeException(r.getStatusCode().toString());
        }
    }

//  ● View Enrolments - view all the courses you are enrolled in.
//  ● Graduation - view eligibility to graduate (must not have any outstanding invoices).

    public FinanceAccount getOrCreateFinanceAccount(String id){
        try{
            return financeIntegrationService.getAccount(id);}
        catch (Exception e){
            //if couldnt get account (didnt exist) make an account
            return financeIntegrationService.createFinanceAccount(new FinanceAccount(id));
            //if this fails it will throw due to a finance server connection issue
        }
    }
    public String showAccount(Model model) {
        //for when signed out / id like to have these as the same object but @Valid issues occur
        model.addAttribute("signInInfo",new SignInInfo());
        model.addAttribute("registerInfo",new RegisterInfo());


        Student sessionStu = getSessionUser(model);
        //if logged in, validate has finance and library accounts and load invoices
        if(sessionStu.dataFilled()) {
            //refresh, since when entering frm Courses after enrolling in a course it dooesnt show
            model.addAttribute("student",getStudentById(sessionStu.getId()));
            sessionStu = getSessionUser(model);
            model.addAttribute("financeConnected",false);
            model.addAttribute("studentInvoices", new ArrayList<Invoice>());

            //throws if cant get account
            try {
                FinanceAccount acc = getOrCreateFinanceAccount(sessionStu.getExternalStudentId());
                model.addAttribute("financeConnected", true);

                List<Invoice> invs = financeIntegrationService.getAllInvoices();
                List<Invoice> stuInvs = new ArrayList<>(List.of());
                model.addAttribute("outstanding", false);
                for (Invoice inv : invs) {

                    if (inv.getAccount().equals(acc)) {
                        stuInvs.add(inv);
                        if (inv.getStatus().equals(Invoice.Status.OUTSTANDING)) model.addAttribute("outstanding", true);
                    }
                }
                model.addAttribute("studentInvoices", stuInvs);
            }catch (Exception ignored){}
        }
        return "student";
    }

//  ● View/Update Student Profile - view profile (includes student ID), update name and
//    surname.

    public String showEditAccount(Model model) {
        //for when changing details
        Student sessionStu = getSessionUser(model);
        //clone session student for if edit mode. would be better in its own page i think
        Student upd = new Student();
        upd.setExternalStudentId(sessionStu.getExternalStudentId());
        upd.setForename(sessionStu.getForename());
        upd.setSurname(sessionStu.getSurname());
        model.addAttribute("updatedStudent",upd);
        return "editStudent";
    }
    public String editAccount(Student updStu, BindingResult bindingResult, Model model) {
        if(bindingResult.hasErrors()){
            return "editStudent";
        }

        Student ses = getSessionUser(model);
        updStu.setExternalStudentId(ses.getExternalStudentId());
        updStu.setCoursesEnrolledIn(ses.getCoursesEnrolledIn());
        if (editStudentJson(ses.getId(),updStu).getStatusCode().equals(HttpStatus.OK)){
            return showAccount(model);
        }
        return "editStudent";
    }



}
