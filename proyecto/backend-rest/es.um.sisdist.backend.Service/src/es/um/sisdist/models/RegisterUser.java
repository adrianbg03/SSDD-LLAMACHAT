package es.um.sisdist.models;

public class RegisterUser {
    private String email;
    private String password;
    private String name;

    // Constructor por defecto
    public RegisterUser() {
    }

    // Constructor con parámetros
    public RegisterUser(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RegisterUser{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}