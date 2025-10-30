package org.itmo.repository;

import org.itmo.domain.City;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;

public interface CityRepository
        extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {


    long countByGovernor_Id(Long humanId);
    long countByCoordinates_Id(Long coordinatesId);

    @Query("select c.id from City c where c.governor.id = :humanId")
    List<Long> findIdsByGovernorId(@Param("humanId") Long humanId);

    @Query("select c.id from City c where c.coordinates.id = :coordId")
    List<Long> findIdsByCoordinatesId(@Param("coordId") Long coordId);

    @Override
    Page<City> findAll(@Nullable Specification<City> spec, Pageable pageable);


    @Query(
            value = "select c from City c left join c.coordinates coord order by coord.x asc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesXAsc(Pageable pageable);

    @Query(
            value = "select c from City c left join c.coordinates coord order by coord.x desc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesXDesc(Pageable pageable);

    @Query(
            value = "select c from City c left join c.coordinates coord order by coord.y asc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesYAsc(Pageable pageable);

    @Query(
            value = "select c from City c left join c.coordinates coord order by coord.y desc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesYDesc(Pageable pageable);


    @Query(
            value = "select c from City c left join c.governor g order by g.height asc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByGovernorHeightAsc(Pageable pageable);

    @Query(
            value = "select c from City c left join c.governor g order by g.height desc",
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByGovernorHeightDesc(Pageable pageable);


    @Query(
            value = """
                select c from City c
                left join c.coordinates coord
                order by
                    case when coord.id is null then 1 else 0 end,
                    coord.id asc,
                    c.id asc
                """,
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesIdAsc(Pageable pageable);

    @Query(
            value = """
                select c from City c
                left join c.coordinates coord
                order by
                    case when coord.id is null then 0 else 1 end,
                    coord.id desc,
                    c.id asc
                """,
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByCoordinatesIdDesc(Pageable pageable);



    @Query(
            value = """
                select c from City c
                left join c.governor g
                order by
                    case when g.id is null then 1 else 0 end,
                    g.id asc,
                    c.id asc
                """,
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByGovernorIdAsc(Pageable pageable);

    @Query(
            value = """
                select c from City c
                left join c.governor g
                order by
                    case when g.id is null then 0 else 1 end,
                    g.id desc,
                    c.id asc
                """,
            countQuery = "select count(c) from City c"
    )
    Page<City> findAllOrderByGovernorIdDesc(Pageable pageable);

    Page<City> findAllByOrderByIdAsc(Pageable pageable);
    Page<City> findAllByOrderByIdDesc(Pageable pageable);
}
