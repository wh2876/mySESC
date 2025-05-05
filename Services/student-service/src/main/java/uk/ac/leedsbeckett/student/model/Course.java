package uk.ac.leedsbeckett.student.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.util.Set;

//marked as Entity so it can be saved into the repositories correctly
@Entity
//marked Data to add get/set methods
//this makes it easier to access the variables since I don't need to remember what they were called and can just type "obj.get" and have it autofill the variables
//I only realised how helpful get and set methods really were after using lombok. I'm probably going to make a point of using them in all my projects from now on.
@Data
public class Course {

    //mark it as the Id field for the MySQL table
    @Id
    //and indicate that the Id is generated upon adding to the repository and how
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Double fee;

    //join this table to the student table through a manytomany connection with their coursesEnrolledIn field
    @ManyToMany(mappedBy = "coursesEnrolledIn")
    @JsonIgnore
    @ToString.Exclude
    Set<Student> studentsEnrolledInCourse;

}
