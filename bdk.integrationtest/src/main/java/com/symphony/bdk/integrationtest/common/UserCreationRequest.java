package com.symphony.bdk.integrationtest.common;

import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.gs.ti.wpt.lc.security.cryptolib.PBKDF;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserCreationRequest {
  private String username;
  private String firstName;
  private String lastName;
  private String surname;
  private String title;
  private String company;
  private String location;
  private List<Role> roles;
  private Map<String, Boolean> entitlement;
  private Boolean person;
  private String imageUrl;
  private String imageUrlSmall;
  private String emailAddress;
  private String displayName;
  private String workPhone;
  private String mobilePhone;
  private String twoFactorAuthPhone;
  private String jobFunction;
  private String department;
  private String division;
  private String salt;
  private String hPassword;
  private String kSalt;
  private String khPassword;
  private Boolean hasPassword = false;
  private Boolean newPassword = false;
  private Boolean active;

  public UserCreationRequest() {
  }

  public static void assignPassword(UserCreationRequest req, String pw) throws SymphonyEncryptionException, SymphonyInputException {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    String s = Base64.encodeBase64String(salt);
    req.salt = s;
    req.kSalt = s;
    String s1 = Base64.encodeBase64String(PBKDF.PBKDF2_SHA256(pw.getBytes(), Base64.decodeBase64(s), 10000));
    req.hPassword = s1;
    req.khPassword = s1;
    req.hasPassword = true;
    req.newPassword = true;
  }

  public List<Role> getRoles() {
    return this.roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  public Map<String, Boolean> getEntitlement() {
    return this.entitlement;
  }

  public void setEntitlement(Map<String, Boolean> entitlement) {
    this.entitlement = entitlement;
  }

  public Boolean isPerson() {
    return this.person;
  }

  public void setPerson(Boolean person) {
    this.person = person;
  }

  public Boolean isActive() {
    return this.active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getImageUrl() {
    return this.imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getImageUrlSmall() {
    return this.imageUrlSmall;
  }

  public void setImageUrlSmall(String imageUrlSmall) {
    this.imageUrlSmall = imageUrlSmall;
  }

  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCompany() {
    return this.company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getLocation() {
    return this.location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmailAddress() {
    return this.emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getSalt() {
    return this.salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  public String gethPassword() {
    return this.hPassword;
  }

  public void sethPassword(String hPassword) {
    this.hPassword = hPassword;
  }

  public String getkSalt() {
    return this.kSalt;
  }

  public void setkSalt(String kSalt) {
    this.kSalt = kSalt;
  }

  public String getKhPassword() {
    return this.khPassword;
  }

  public void setKhPassword(String khPassword) {
    this.khPassword = khPassword;
  }

  @JsonProperty("hasPassword")
  public Boolean getHasPassword() {
    return this.hasPassword;
  }

  public void setHasPassword(Boolean hasPassword) {
    this.hasPassword = hasPassword;
  }

  public Boolean getNewPassword() {
    return this.newPassword;
  }

  public void setNewPassword(Boolean newPassword) {
    this.newPassword = newPassword;
  }

  public String getWorkPhone() {
    return this.workPhone;
  }

  public void setWorkPhone(String workPhone) {
    this.workPhone = workPhone;
  }

  public String getMobilePhone() {
    return this.mobilePhone;
  }

  public void setMobilePhone(String mobilePhone) {
    this.mobilePhone = mobilePhone;
  }

  public String getJobFunction() {
    return this.jobFunction;
  }

  public void setJobFunction(String jobFunction) {
    this.jobFunction = jobFunction;
  }

  public String getDepartment() {
    return this.department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getDivision() {
    return this.division;
  }

  public void setDivision(String division) {
    this.division = division;
  }

  public String getSurname() {
    return this.surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getTwoFactorAuthPhone() {
    return this.twoFactorAuthPhone;
  }

  public void setTwoFactorAuthPhone(String twoFactorAuthPhone) {
    this.twoFactorAuthPhone = twoFactorAuthPhone;
  }
}