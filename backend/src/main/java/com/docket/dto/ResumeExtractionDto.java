package com.docket.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public class ResumeExtractionDto {

    public static class ExperienceDto {
        @NotNull(message = "company is required")
        private String company;

        @NotNull(message = "role is required")
        private String role;

        @NotNull(message = "duration is required")
        private String duration;

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }

    @NotNull(message = "candidateName is required")
    private String candidateName;

    @NotNull(message = "email is required")
    private String email;

    @NotNull(message = "phone is required")
    private String phone;

    @NotNull(message = "skills is required")
    private List<String> skills;

    @NotNull(message = "experience is required")
    @Valid
    private List<ExperienceDto> experience;

    @NotNull(message = "education is required")
    private String education;

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<ExperienceDto> getExperience() { return experience; }
    public void setExperience(List<ExperienceDto> experience) { this.experience = experience; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
}
