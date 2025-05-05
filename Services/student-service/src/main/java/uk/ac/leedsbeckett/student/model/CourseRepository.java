package uk.ac.leedsbeckett.student.model;

import org.springframework.data.jpa.repository.JpaRepository;
//make a repository to access the course database
public interface CourseRepository extends JpaRepository<Course, Long>{
    Course getCourseByDescription(String description); //can't remember why I defined this, but I don't want to get rid of it just in case it somehow breaks everything
}