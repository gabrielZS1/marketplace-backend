package com.marketplace.backend.dto;

import java.util.List;

public class EmployeeUpdateRequestDTO {

    private String name;
    private String phone;
    private String bio;
    private String specialty;

    private List<WorkingHourRequestItemDTO> workingHours;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public List<WorkingHourRequestItemDTO> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(
            List<WorkingHourRequestItemDTO> workingHours
    ) {
        this.workingHours = workingHours;
    }
}