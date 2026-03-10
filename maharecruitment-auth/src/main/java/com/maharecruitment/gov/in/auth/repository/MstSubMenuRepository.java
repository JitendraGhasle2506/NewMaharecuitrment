package com.maharecruitment.gov.in.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;

@Repository
public interface MstSubMenuRepository extends JpaRepository<MstSubMenu, Long> {

    List<MstSubMenu> findAllByOrderByMenuMenuIdAscSubMenuIdAsc();

    List<MstSubMenu> findAllByOrderBySubMenuIdAsc();

    List<MstSubMenu> findByMenuMenuIdInOrderByMenuMenuIdAscSubMenuIdAsc(List<Long> menuIds);

    List<MstSubMenu> findByMenuMenuIdInAndIsActiveOrderByMenuMenuIdAscSubMenuIdAsc(List<Long> menuIds, Character isActive);

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
