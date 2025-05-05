package uk.ac.leedsbeckett.student.controller;


import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.leedsbeckett.student.model.Course;
import uk.ac.leedsbeckett.student.service.CourseService;

//declare controller as rest controller so it can be accessed with postman and return responseEntitys properly
@RestController
@RequestMapping("/api/course") //request this uri snippet be added to the localhost:XXXX/student uri call to filter it to the course service
public class CourseController {

    private CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    //mark this as a get request and map it toe localhost:XXXX/student/api/course/{courseId}
    @GetMapping("/{courseId}")
    @ResponseBody//indicate this returns a response with a body
    //where courseId is used as a parameter in the method (@PathVariable)
    //it returns ResponseEntity<?> because it could contain eithre a course or a problem (i.e. no course with this id)
    public ResponseEntity<?> getCourseJson(@PathVariable Long courseId){
        //call the service method
        return courseService.getCourseByIdJson(courseId);
    }

    @GetMapping("/courses")
    @ResponseBody
    public ResponseEntity<CollectionModel<EntityModel<Course>>> getCoursesJson(){
        return courseService.getAllCoursesJson();
    }

    @PostMapping("/createCourse")
    @ResponseBody
    public ResponseEntity<?> createCourseJson(@RequestBody Course course){
        return courseService.createNewCourseJson(course);
    }

    @PutMapping("/editCourse/{courseId}")
    @ResponseBody
    public ResponseEntity<?> editCourseJson(@PathVariable Long courseId, @RequestBody Course course){
        return courseService.editCourseJson(courseId, course);
    }
}
