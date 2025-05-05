package uk.ac.leedsbeckett.student.controller;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.leedsbeckett.student.model.Student;
import uk.ac.leedsbeckett.student.service.StudentService;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/{studentId}")
    @ResponseBody
    public ResponseEntity<?> getStudentJson(@PathVariable Long studentId){
        return studentService.getStudentByIdJson(studentId);
    }

    @GetMapping("/students")
    @ResponseBody
    public ResponseEntity<CollectionModel<EntityModel<Student>>> getStudentsJson(){
        return studentService.getAllStudentsJson();
    }

    @PostMapping("/createStudent")
    @ResponseBody
    public ResponseEntity<?> createStudentJson(@RequestBody Student student){
        return studentService.createNewStudentJson(student);
    }

    @PutMapping("/editStudent/{studentId}")
    @ResponseBody
    public ResponseEntity<?> editStudentJson(@PathVariable Long studentId, @RequestBody Student student){
        return studentService.editStudentJson(studentId, student);
    }

    @DeleteMapping("/deleteStudent/{studentId}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long studentId){
        return studentService.deleteStudentJson(studentId);
    }

    @PutMapping("/{studentId}/addCourse/{courseId}")
    @ResponseBody
    public ResponseEntity<?> enrollInCourseJson(@PathVariable Long studentId, @PathVariable Long courseId){
       return studentService.enrollStudentJson(studentId, courseId);
    }

    @DeleteMapping("/{studentId}/delCourse/{courseId}")
    @ResponseBody
    public ResponseEntity<?> disenrollFromCourseJson(@PathVariable Long studentId, @PathVariable Long courseId){
        return studentService.disenrollStudentJson(studentId, courseId);
    }
}
