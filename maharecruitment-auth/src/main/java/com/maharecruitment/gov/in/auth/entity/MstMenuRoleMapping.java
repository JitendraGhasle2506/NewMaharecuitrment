package com.maharecruitment.gov.in.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "menu_role_mapping")
@NoArgsConstructor
public class MstMenuRoleMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mstmenurolemapping_seq_generator")
    @SequenceGenerator(name = "mstmenurolemapping_seq_generator", sequenceName = "mstMenuRoleMapping_seq", allocationSize = 1)
    @Column(name = "MENU_MAP_ID")
    private Long menuMapID;

    @Column(name = "is_active")
    private Character isActive;

    @ManyToOne
    @JoinColumn(name = "MENU_ID")
    private MstMenu menu;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    public MstMenuRoleMapping(Character isActive) {
        this.isActive = isActive;
    }
}
