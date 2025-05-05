package uk.ac.leedsbeckett.student.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="Id")
    private Long id;

    @Column(unique=true)
    private String externalStudentId;

    //validation is only for edit student portal ability, so only validate editable attributes
    @Size(min=1, message = "{name.size}")
    @Pattern(regexp = "[a-zA-Z]+(-[a-zA-Z]+)?", message = "{name.format}")
    private String surname;

    @Size(min=1, message = "{name.size}")
    @Pattern(regexp = "[a-zA-Z]+(-[a-zA-Z]+)?", message = "{name.format}")
    private String forename;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_student",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
            )
    @EqualsAndHashCode.Exclude
    Set<Course> coursesEnrolledIn = new HashSet<>();

    public Student() {}
    public Student(Student stu) {
        this.id = stu.getId();
        this.surname = stu.getSurname();
        this.forename = stu.getForename();
        this.externalStudentId = stu.getExternalStudentId();
        this.coursesEnrolledIn.addAll(stu.getCoursesEnrolledIn());
    }

    public boolean enrolInCourse(Course course) {
        return coursesEnrolledIn.add(course); //add() handles returning true/false if successful add or not
    }
    public boolean isInCourse(Course course) {
        return coursesEnrolledIn.contains(course);
    }
    public boolean removeCourse(Course course) {
        return coursesEnrolledIn.remove(course);
    }

    public boolean dataFilled(){
        return id != null
                && externalStudentId != null
                && surname != null
                && forename != null
                && coursesEnrolledIn != null;
    }
}
