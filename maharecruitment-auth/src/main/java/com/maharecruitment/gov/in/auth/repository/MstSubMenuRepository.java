package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;

@Repository
public interface MstSubMenuRepository extends JpaRepository<MstSubMenu, Long> {

    List<MstSubMenu> findAllByOrderByMenuMenuIdAscSubMenuIdAsc();

    List<MstSubMenu> findAllByOrderBySubMenuIdAsc();

    @EntityGraph(attributePaths = { "menu", "roles" })
    @Query(
            value = """
                    select sm
                    from MstSubMenu sm
                    left join sm.menu menu
                    order by upper(coalesce(menu.menuNameEnglish, '')),
                             upper(coalesce(sm.subMenuNameEnglish, '')),
                             sm.subMenuId
                    """,
            countQuery = "select count(sm) from MstSubMenu sm")
    Page<MstSubMenu> findAllWithMenuAndRoles(Pageable pageable);

    @EntityGraph(attributePaths = { "menu", "roles" })
    @Query(
            value = """
                    select sm
                    from MstSubMenu sm
                    left join sm.menu menu
                    where upper(coalesce(menu.menuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.subMenuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.subMenuNameMarathi, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.controllerName, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.url, '')) like upper(concat('%', :searchTerm, '%'))
                       or exists (
                            select matchedRole.id
                            from MstSubMenu smRole
                            join smRole.roles matchedRole
                            where smRole.subMenuId = sm.subMenuId
                              and upper(coalesce(matchedRole.name, '')) like upper(concat('%', :searchTerm, '%'))
                       )
                       or (:normalizedSearch = 'ACTIVE' and coalesce(sm.isActive, 'Y') = 'Y')
                       or (:normalizedSearch = 'INACTIVE' and coalesce(sm.isActive, 'Y') = 'N')
                    order by upper(coalesce(menu.menuNameEnglish, '')),
                             upper(coalesce(sm.subMenuNameEnglish, '')),
                             sm.subMenuId
                    """,
            countQuery = """
                    select count(sm)
                    from MstSubMenu sm
                    left join sm.menu menu
                    where upper(coalesce(menu.menuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.subMenuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.subMenuNameMarathi, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.controllerName, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(sm.url, '')) like upper(concat('%', :searchTerm, '%'))
                       or exists (
                            select matchedRole.id
                            from MstSubMenu smRole
                            join smRole.roles matchedRole
                            where smRole.subMenuId = sm.subMenuId
                              and upper(coalesce(matchedRole.name, '')) like upper(concat('%', :searchTerm, '%'))
                       )
                       or (:normalizedSearch = 'ACTIVE' and coalesce(sm.isActive, 'Y') = 'Y')
                       or (:normalizedSearch = 'INACTIVE' and coalesce(sm.isActive, 'Y') = 'N')
                    """)
    Page<MstSubMenu> searchAllWithMenuAndRoles(
            @Param("searchTerm") String searchTerm,
            @Param("normalizedSearch") String normalizedSearch,
            Pageable pageable);

    @EntityGraph(attributePaths = { "menu", "roles" })
    Optional<MstSubMenu> findBySubMenuId(Long subMenuId);

    List<MstSubMenu> findByMenuMenuIdInOrderByMenuMenuIdAscSubMenuIdAsc(List<Long> menuIds);

    List<MstSubMenu> findByMenuMenuIdInAndIsActiveOrderByMenuMenuIdAscSubMenuIdAsc(List<Long> menuIds, Character isActive);

    @EntityGraph(attributePaths = { "menu" })
    @Query("""
            select distinct sm
            from MstSubMenu sm
            join sm.menu m
            left join sm.roles sr
            left join m.roles mr
            where m.menuId in :menuIds
              and (
                    sr.id in :roleIds
                    or (sr.id is null and mr.id in :roleIds)
                  )
            """)
    List<MstSubMenu> findVisibleSubMenusByMenuIdsAndRoleIds(
            @Param("menuIds") List<Long> menuIds,
            @Param("roleIds") List<Long> roleIds);

    @EntityGraph(attributePaths = { "menu" })
    @Query("""
            select distinct sm
            from MstSubMenu sm
            join sm.menu m
            left join sm.roles sr
            left join m.roles mr
            where m.menuId in :menuIds
              and (
                    upper(sr.name) in :roleNames
                    or (sr.id is null and upper(mr.name) in :roleNames)
                  )
            """)
    List<MstSubMenu> findVisibleSubMenusByMenuIdsAndRoleNames(
            @Param("menuIds") List<Long> menuIds,
            @Param("roleNames") List<String> roleNames);

    Optional<MstSubMenu> findByMenuMenuIdAndSubMenuNameEnglishIgnoreCase(Long menuId, String subMenuNameEnglish);

    boolean existsByMenuMenuIdAndSubMenuNameEnglishIgnoreCase(Long menuId, String subMenuNameEnglish);

    boolean existsByMenuMenuIdAndSubMenuNameEnglishIgnoreCaseAndSubMenuIdNot(
            Long menuId,
            String subMenuNameEnglish,
            Long subMenuId);

    boolean existsByUrlIgnoreCase(String url);

    boolean existsByUrlIgnoreCaseAndSubMenuIdNot(String url, Long subMenuId);

    long countByMenuMenuId(Long menuId);
}
