package com.maharecruitment.gov.in.auth.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "MstMenu")
@NoArgsConstructor
public class MstMenu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_name_english")
    private String menuNameEnglish;

    @Column(name = "menu_name_marathi")
    private String menuNameMarathi;

    @Column(name = "is_active")
    private String isActive;

    @Column(name = "icon")
    private String icon;

    @Column(name = "url")
    private String url;

    @Column(name = "is_sub_menu", columnDefinition = "int default 0")
    private Integer isSubMenu;

    @Transient
    private List<Long> roleIds = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "menu_role", joinColumns = @JoinColumn(name = "menu_id"), inverseJoinColumns = @JoinColumn(name = "id"))
    private Set<Role> roles = new HashSet<>();
}
