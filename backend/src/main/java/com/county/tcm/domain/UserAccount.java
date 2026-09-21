package com.county.tcm.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 40)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 所属机构:诊所名称 / 配送班组等 */
    private String organization;

    @Column(length = 20)
    private String phone;

    private boolean enabled = true;

    public UserAccount() {}

    public UserAccount(String username, String password, String displayName, Role role, String organization, String phone) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.organization = organization;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public String getOrganization() { return organization; }
    public String getPhone() { return phone; }
    public boolean isEnabled() { return enabled; }

    public void setPassword(String password) { this.password = password; }
}
