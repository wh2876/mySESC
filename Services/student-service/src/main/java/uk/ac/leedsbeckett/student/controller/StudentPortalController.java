package uk.ac.leedsbeckett.student.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import uk.ac.leedsbeckett.student.model.RegisterInfo;
import uk.ac.leedsbeckett.student.model.SignInInfo;
import uk.ac.leedsbeckett.student.model.Student;
import uk.ac.leedsbeckett.student.service.StudentService;

//regular controller since it gives html-used returns rather than response entities
@Controller
//declare an object to be preserved across a whole webpage session when a user uses it
@SessionAttributes("student")
public class StudentPortalController {

    //declare definition of session attribute
    @ModelAttribute("student")
    public Student getSessionStudent() {
        return new Student();
    }

    private final StudentService studentService;

    public StudentPortalController(StudentService studentService) {
        this.studentService = studentService;
    }

    //these methods return strings used by thymeleaf to deccide with html file to load.
    //if thymeleaf wasnt injected by maven then the webpage would just display the string returned
    @GetMapping( "/")
    public String redirectToHome(Model model) {
        return "redirect:/portal";
    }

    @GetMapping( "/portal")
    public String showPortal(Model model) {
        return studentService.showPortal(model);
    }

    @GetMapping("/portal/account")
    public String showAccount(Model model) {
        return studentService.showAccount(model);
    }
    @PostMapping("/portal/account")
    public String logIn(@ModelAttribute @Valid SignInInfo logInCredentials, BindingResult bindingResult, Model model) {
        return studentService.attemptLogIn(logInCredentials.getExternalStudentId(), bindingResult, model);
    }
    @PostMapping("/portal/account/logout")
    public String logOut(Model model) {
        System.out.println("logOut");
        return studentService.logOut(model);
    }
    @PostMapping("/portal/account/register")
    public String registerStudent(@ModelAttribute @Valid RegisterInfo logInCredentials, BindingResult bindingResult, Model model) {
        return studentService.registerPortalUser(logInCredentials.getForename(),logInCredentials.getSurname(),bindingResult, model);
    }
    @GetMapping("/portal/account/edit")
    public String editStudentInfo(Model model) {
        return studentService.showEditAccount(model);
    }
    @PostMapping("/portal/account/edit")
    public String finaliseEditInfo(@ModelAttribute("updatedStudent") @Valid Student updatedStudent, BindingResult bindingResult, Model model) {
        return studentService.editAccount(updatedStudent,bindingResult,model);
    }

    @GetMapping("/portal/courses")
    public String showCourses(Model model) {
        return studentService.showAvailableCourses(model);
    }
    @PostMapping("/portal/courses")
    public String enrollOrDisenrollCourses(@RequestParam("courseId") Long course, Model model) {
        return studentService.enrolInCourse(course,model);
    }
}
