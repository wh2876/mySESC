package uk.ac.leedsbeckett.student.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SignInInfo {
    //signInInfo follows same principles as RegisterInfo with validation
    @NotBlank(message = "{externalStudentId.required}")
    @Size(min = 8, max = 8, message = "{externalStudentId.size}")
    @Pattern(regexp = "c[0-9]*", message = "{externalStudentId.format}")
    private String externalStudentId;
    //its worth noting this should obviously NOT be a final signininfo object in a real student portal because there are no security measures
    //students dont have to register an email to prevent duplicate accounts, and dont need a password to log into other students accounts

    public SignInInfo(){}
}
