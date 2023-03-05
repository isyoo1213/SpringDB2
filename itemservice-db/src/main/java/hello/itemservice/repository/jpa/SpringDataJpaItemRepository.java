package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// *** 이러한 Interface의 구현은 스프링이 Proxy를 통해 만든 구현 클래스의 인스턴스를 Bean 등록한다

// *** SDJ는 Interface를 기반으로 작성한다
public interface SpringDataJpaItemRepository extends JpaRepository<Item, Long> {
    // 1st : 관리할 Entity
    // 2nd : Entity의 PK의 Type
    // -> 기본적인 CRUD는 완성 -> 필요한 기능만 작성

    List<Item> findByItemNameLike(String itemName);

    List<Item> findByPriceLessThanEqual(Integer price);

    // 위의 두 조건을 합친 쿼리 메서드 -> Join 사용 불가능
    List<Item> findByItemNameLikeAndPriceLessThanEqual(String itemName, Integer price);

    // 쿼리 직접 실행 - jpql 직접 작성한 것과 마찬가지 + 위의 쿼리 메서드와 동일한 기능 수행
    @Query("select i from Item i where i.itemName like :itemName and i.price <= :price")
    List<Item> findItems(@Param("itemName") String itemName, @Param("price") Integer price);
    // *** @Param의 패키지 확인
    // *** 이렇게 쿼리를 직접 실행할 경우 - Parameter는 명시적으로 @Param 어노테이션을 통해 바인딩 해줘야 함
    // -> 문자열은 이름 기반 parameter Binding과 매핑됨 ex) "itemName"의 parameter -> :itemName

    // *** 현재의 조회기능을 메서드로 작성한 현황
    // 모든 데이터 / 이름 / 가격 / 이름 + 가격
    // -> 동적 쿼리를 사용해서 이를 한번에 묶을 수는 없을까? -> SDJ는 jpql동적 쿼리에 취약
    // -> 이후 Querydls로 깔끔하게 해결할 예정

}
