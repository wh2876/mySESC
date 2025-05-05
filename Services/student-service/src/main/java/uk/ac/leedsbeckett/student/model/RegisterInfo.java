package uk.ac.leedsbeckett.student.model;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class RegisterInfo {
    //validation criteria checked by the validation dependency when obj is marked with @Valid when taken as a parameter.
    //validation problems are tied to a bindingResult object that MUST be the following parameter when @Valid is used
    // name.size etc. are defined in messages.properties
    @Size(min=1, message = "{name.size}")
    //names should be only letters, with an exception for 1 hyphen if more letters follow i.e. Carol-Ann is my mothers first name
    //this may be too rigid but I didn't want people entering names like "£$%^&" or something so some sort of validation was necessary
    @Pattern(regexp = "[a-zA-Z]+(-[a-zA-Z]+)?", message = "{name.format}")
    private String surname;

    @Size(min=1, message = "{name.size}")
    @Pattern(regexp = "[a-zA-Z]+(-[a-zA-Z]+)?", message = "{name.format}")
    private String forename;

    public RegisterInfo() { }
}
