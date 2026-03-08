package com.maharecruitment.gov.in.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.MstMenu;

@Repository
public interface MstMenuRepository extends JpaRepository<MstMenu, Long> {

    @Query("""
            select distinct m
            from MstMenu m
            join m.roles r
            where r.id in :roleIds
            order by m.menuId
            """)
    List<MstMenu> findMenusByRoleIds(@Param("roleIds") List<Long> roleIds);
}
