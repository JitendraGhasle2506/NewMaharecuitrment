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

import com.maharecruitment.gov.in.auth.entity.MstMenu;

@Repository
public interface MstMenuRepository extends JpaRepository<MstMenu, Long> {

    Optional<MstMenu> findByMenuNameEnglishIgnoreCase(String menuNameEnglish);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select m
            from MstMenu m
            where upper(m.menuNameEnglish) = upper(:menuNameEnglish)
            """)
    Optional<MstMenu> findByMenuNameEnglishIgnoreCaseWithRoles(@Param("menuNameEnglish") String menuNameEnglish);

    Optional<MstMenu> findByUrlIgnoreCase(String url);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select m
            from MstMenu m
            where upper(m.url) = upper(:url)
            """)
    Optional<MstMenu> findByUrlIgnoreCaseWithRoles(@Param("url") String url);

    boolean existsByMenuNameEnglishIgnoreCase(String menuNameEnglish);

    boolean existsByMenuNameEnglishIgnoreCaseAndMenuIdNot(String menuNameEnglish, Long menuId);

    boolean existsByUrlIgnoreCase(String url);

    boolean existsByUrlIgnoreCaseAndMenuIdNot(String url, Long menuId);

    @EntityGraph(attributePaths = "roles")
    @Query("select m from MstMenu m")
    Page<MstMenu> findAllWithRoles(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query(
            value = """
                    select distinct m
                    from MstMenu m
                    left join m.roles r
                    where upper(coalesce(m.menuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(m.menuNameMarathi, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(m.url, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(r.name, '')) like upper(concat('%', :searchTerm, '%'))
                       or ((:normalizedSearch = 'PARENT' or :normalizedSearch = 'PARENT MENU')
                           and coalesce(m.isSubMenu, 0) = 0)
                       or ((:normalizedSearch = 'DIRECT' or :normalizedSearch = 'DIRECT LINK')
                           and coalesce(m.isSubMenu, 0) = 1)
                       or (:normalizedSearch = 'ACTIVE' and upper(coalesce(m.isActive, 'Y')) = 'Y')
                       or (:normalizedSearch = 'INACTIVE' and upper(coalesce(m.isActive, 'Y')) = 'N')
                    order by m.menuNameEnglish, m.menuId
                    """,
            countQuery = """
                    select count(distinct m.menuId)
                    from MstMenu m
                    left join m.roles r
                    where upper(coalesce(m.menuNameEnglish, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(m.menuNameMarathi, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(m.url, '')) like upper(concat('%', :searchTerm, '%'))
                       or upper(coalesce(r.name, '')) like upper(concat('%', :searchTerm, '%'))
                       or ((:normalizedSearch = 'PARENT' or :normalizedSearch = 'PARENT MENU')
                           and coalesce(m.isSubMenu, 0) = 0)
                       or ((:normalizedSearch = 'DIRECT' or :normalizedSearch = 'DIRECT LINK')
                           and coalesce(m.isSubMenu, 0) = 1)
                       or (:normalizedSearch = 'ACTIVE' and upper(coalesce(m.isActive, 'Y')) = 'Y')
                       or (:normalizedSearch = 'INACTIVE' and upper(coalesce(m.isActive, 'Y')) = 'N')
                    """)
    Page<MstMenu> searchAllWithRoles(
            @Param("searchTerm") String searchTerm,
            @Param("normalizedSearch") String normalizedSearch,
            Pageable pageable);

    List<MstMenu> findAllByOrderByMenuNameEnglishAsc();

    List<MstMenu> findByIsSubMenuOrderByMenuNameEnglishAsc(Integer isSubMenu);

    List<MstMenu> findByIsSubMenuAndIsActiveIgnoreCaseOrderByMenuNameEnglishAsc(Integer isSubMenu, String isActive);

    List<MstMenu> findByIsActiveIgnoreCaseOrderByMenuNameEnglishAsc(String isActive);

    @Query("""
            select distinct m
            from MstMenu m
            join m.roles r
            where r.id in :roleIds
              and upper(coalesce(m.isActive, 'Y')) = 'Y'
            order by m.menuId
            """)
    List<MstMenu> findMenusByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Query("""
            select distinct m
            from MstMenu m
            join m.roles r
            where r.id = :roleId
            order by m.menuId
            """)
    List<MstMenu> findMenusByRoleId(@Param("roleId") Long roleId);

    @Query("""
            select r.id, count(m.menuId)
            from MstMenu m
            join m.roles r
            where r.id in :roleIds
              and upper(coalesce(m.isActive, 'Y')) = 'Y'
            group by r.id
            """)
    List<Object[]> countMenusByRoleIds(@Param("roleIds") List<Long> roleIds);
}
