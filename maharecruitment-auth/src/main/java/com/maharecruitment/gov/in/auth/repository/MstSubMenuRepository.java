package com.maharecruitment.gov.in.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;

@Repository
public interface MstSubMenuRepository extends JpaRepository<MstSubMenu, Long> {

    List<MstSubMenu> findAllByOrderByMenuMenuIdAscSubMenuIdAsc();

    List<MstSubMenu> findByMenuMenuIdInOrderByMenuMenuIdAscSubMenuIdAsc(List<Long> menuIds);
}
