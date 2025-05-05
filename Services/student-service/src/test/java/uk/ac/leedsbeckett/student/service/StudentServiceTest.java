package uk.ac.leedsbeckett.student.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.ac.leedsbeckett.student.model.Student;
import uk.ac.leedsbeckett.student.model.StudentRepository;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setExternalStudentId("c1111111");
        student.setForename("John");
        student.setSurname("Doe");
        student.setCoursesEnrolledIn(Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        studentRepository.deleteAll();
    }

    @Test
    void getNumFromStudentId() {
        String studentId = "c0103050";
        assertThat(studentService.getNumFromStudentId(studentId)).isEqualTo(103050);
    }

    @Test
    void makeIdFromNum() {
        int studentIdNum = 103050;
        assertThat(studentService.makeIdFromNum(studentIdNum)).isEqualTo("c0103050");
    }

    @Test
    void generateNewStudentId() {
        Mockito.when(studentRepository.findAll()).thenReturn(List.of(student)); //makes the only unavailable id c1111111
        assertThat(studentService.generateNewStudentId(null)).isEqualTo("c0000000");
        assertThat(studentService.generateNewStudentId("c0000000")).isEqualTo("c0000000");
        assertThat(studentService.generateNewStudentId("c1111111")).isEqualTo("c1111112");

    }
}