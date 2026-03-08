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
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "sub_menu_mst")
public class MstSubMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mstsubmenu_seq_generator")
    @SequenceGenerator(name = "mstsubmenu_seq_generator", sequenceName = "mstsubmenu_seq", allocationSize = 1)
    @Column(name = "SUB_MENU_ID")
    private Long subMenuId;

    @Column(name = "sub_menu_name_english")
    private String subMenuNameEnglish;

    @Column(name = "controller_name")
    private String controllerName;

    @Column(name = "url")
    private String url;

    @Column(name = "sub_menu_name_marathi")
    private String subMenuNameMarathi;

    @Column(name = "icon")
    private String icon;

    @Column(name = "is_active")
    private Character isActive;

    @ManyToOne
    @JoinColumn(name = "MENU_ID")
    private MstMenu menu;
}
