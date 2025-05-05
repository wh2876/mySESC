package uk.ac.leedsbeckett.student.service;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.leedsbeckett.student.controller.CourseController;
import uk.ac.leedsbeckett.student.model.Course;
import uk.ac.leedsbeckett.student.model.CourseRepository;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

//component to mark it as a bean so parameters are passed in automatically by spring boot and to make sure only 1 of it exists in the program
@Component
public class CourseService extends Service {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    //generic entitymodel method to add standard links
    private EntityModel<Course> getEntityModel(Course course){
        return EntityModel.of(course,
                linkTo(methodOn(CourseController.class).getCourseJson(course.getId())).withSelfRel(),
                linkTo(methodOn(CourseController.class).getCoursesJson()).withRel("courses"));
    }
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    public ResponseEntity<CollectionModel<EntityModel<Course>>> getAllCoursesJson(){
        List<Course> courseList = getAllCourses();
        //convert to collectionmodel of entitymodels
        CollectionModel<EntityModel<Course>> col = CollectionModel.of(courseList
                .stream()
                .map(this::getEntityModel)
                .toList());
        return ResponseEntity.ok(CollectionModel.of(col, linkTo(methodOn(CourseController.class)
                .getCoursesJson())
                .withSelfRel()));
    }

    public Course getCourseById(Long id){
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No course found with id " + id));
    }
    //mentioned in one of the controllers but return ResponseEntity<?> because it may fail or succeed, so it has to be able to return either
    // the request object OR a problem
    public ResponseEntity<?> getCourseByIdJson(Long id){
        try{
            Course course = getCourseById(id);
            return makeResponseEntity(getEntityModel(course));
        }catch (Exception e){
            return getProblemResponse(e);
        }

    }

    public boolean courseIsValid(Course course) {
        //if any field of the course is not filled, course is invalid and cannot be added
        //id does not need to be checked because adding the course to the repo should
        //  change it to match (i.e. if it is the 6th added the id will become 6)
        return course != null
                && course.getTitle() != null
                && course.getDescription() != null
                && course.getFee() != null
                && course.getStudentsEnrolledInCourse() != null;
    }
    public Course createNewCourse(Course course){
        //all data needs to be non-null
        if(!courseIsValid(course)) throw new RuntimeException("Course is not valid");
        else{
            List<Course> courses = getAllCourses();
            for(Course c : courses){
                //not the best duplicate prevention, it could be they are almost the same but one is longer + therefore more expensive
                //it does promote clarity on course names though, i.e. SESC vs SESC (extended) or whatever
                if(c.getTitle().equals(course.getTitle())
                    && c.getDescription().equals(course.getDescription())){
                    throw new RuntimeException("Course already exists");
                }
            }
        }
        return courseRepository.save(course);
    }
    public ResponseEntity<?> createNewCourseJson(Course course){
       try {
           Course addedCourse = createNewCourse(course);
           return makeResponseEntity(getEntityModel(addedCourse));
       }catch (Exception e){
           return getProblemResponse(e);
       }
    }

    public Course editCourse(Long id, Course course){
        if(!courseIsValid(course)) {throw new RuntimeException("Course is not valid");}

        Course editedCourse = getCourseById(id);
        editedCourse.setTitle(course.getTitle());
        editedCourse.setDescription(course.getDescription());
        editedCourse.setFee(course.getFee());
        return courseRepository.save(editedCourse);
    }
    public ResponseEntity<?> editCourseJson(Long id, Course course){
        try {
            Course editedCourse = editCourse(id, course);
            return makeResponseEntity(getEntityModel(editedCourse));
        }catch (Exception e){
            return getProblemResponse(e);
        }
    }
}
