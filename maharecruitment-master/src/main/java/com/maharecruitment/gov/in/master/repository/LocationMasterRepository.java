package com.maharecruitment.gov.in.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.master.entity.LocationMaster;

@Repository
public interface LocationMasterRepository extends JpaRepository<LocationMaster, Long> {

    boolean existsByLocationNameIgnoreCase(String locationName);

    boolean existsByLocationNameIgnoreCaseAndLocationIdNot(String locationName, Long locationId);

    Optional<LocationMaster> findByLocationId(Long locationId);

    List<LocationMaster> findAllByOrderByLocationNameAsc();

    List<LocationMaster> findByActiveFlagIgnoreCaseOrderByLocationNameAsc(String activeFlag);

    Page<LocationMaster> findByActiveFlagIgnoreCase(String activeFlag, Pageable pageable);

    @Query("""
            select location from LocationMaster location
            where lower(location.locationName) like lower(concat('%', :searchText, '%'))
               or lower(coalesce(location.officeName, '')) like lower(concat('%', :searchText, '%'))
               or lower(coalesce(location.departmentName, '')) like lower(concat('%', :searchText, '%'))
            """)
    Page<LocationMaster> searchByLocationNameOrOfficeName(
            @Param("searchText") String searchText,
            Pageable pageable);

    @Query("""
            select location from LocationMaster location
            where lower(location.activeFlag) = lower(:activeFlag)
              and (
                    lower(location.locationName) like lower(concat('%', :searchText, '%'))
                 or lower(coalesce(location.officeName, '')) like lower(concat('%', :searchText, '%'))
                 or lower(coalesce(location.departmentName, '')) like lower(concat('%', :searchText, '%'))
              )
            """)
    Page<LocationMaster> searchByActiveFlagAndLocationNameOrOfficeName(
            @Param("activeFlag") String activeFlag,
            @Param("searchText") String searchText,
            Pageable pageable);
}
