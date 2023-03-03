package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
// *** JPA 사용 시@Repository의 역할
// - @Controller 어노테이션 : @Component + controller와 연관된 기능 보유
// - @Service : 오로지 @Conponent의 기능
// - @Repository : @Component + *** 예외 변환 AOP의 대상
//   -> Spring과 함께 사용시 Spring이 JPA 예외변환기를 등록 * PersistenceExceptionTranslator
//   -> JPA 관련 Exception 발생시 스프링 데이터 접근 예외로 변환
// *** JPA와 @Repository에서 JPA 관련 예외 발생시 처리 과정
//    1. SpringBoot는 @Repository 어노테이션을 통해 * 예외 변환 AOP Proxy를 만듦 - 내부적으로 여러 단계를 거침
//    2. Repository는 JPA Exception인 PersistenceException을 예외 변환 AOP Proxy로 전달
//    3. JPA는 기본적으로 @Repository, @Transactional에 Proxy를 생성해 내부로직 처리
//    4. AOP Proxy는 Repository Proxy 객체를 통해 PersistenceException을 DataAccessException으로 변환
//    5. 변환된 DataAccessException을 Service계층으로 전달 (Service는 스프링예외추상화에 의존)
// -> 이 과정은 Test에서 로그를 통해 확인 가능
@Transactional
// *** JPA의 모든 데이터 변경(조회는 transaction없이 가능)은 Transaction 내에서 이루어짐
// + *** 일반적으로는 비즈니스 로직이 시작되는 계층에서 transaction을 걸어주는 것이 맞음(주로 Service)
public class JpaItemRepository implements ItemRepository {

    // *** JPA의 실질적인 로직을 수행해주는 의존성 EntityManager
    //    - Spring과 통합된 환경이므로 관련 설정을 모두 잡아줌
    //    - 그렇지 않을 경우 EntityManagerFactory, JpaTransactionManager, DataSource 설정 등을 해주어야함
    // * SpringBoot의 자동설정은 JpaBaseConfiguration을 참고

    // *** EntityManager는 순수한 JPA 기술 - 스프링과 관련이 없다
    //    - 오류발생시 PersistenceException과 그 하위 예외인 JPA 예외를 발생시킴
    //    - 추가로 IllegalStateException / IllegalArgumentException도 가능
    // *** EntityManager에서 발생한 예외를 Repository계층에서 핸들링하지 못하면 Service계층이 해당 Exception에 종속적이게 된다
    //    -> 즉, JPA 예외를 기존의 Spring이 처리할 수 있는 스프링 예외 추상화 DataAccessException으로 변환할 수 있어야함
    private final EntityManager em;

    public JpaItemRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Item save(Item item) {
        em.persist(item);
        // JPA가 만들어서 실행한 SQL
        // insert into item (id, item_name, price, quantity) values (default, ?, ?, ?)
        // - strategy에 따라 PK에 null, default, 아무것도 넣지 않을 수도 있다
        // - * 쿼리 실행 이후 Item객체의 id필드에 DB가 생성한 PK값을 넣어줌
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = em.find(Item.class, itemId);
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
        // *** update() 관련 로직을 수행하지 않음
        //     - Transaction이 Commit되는 시점에 변경된 entity 객체가 있는지 확인
        // *** JPA는 Entity 원본 객체를 스냅샷으로 복제해서 가지고 있음
        //     -> 이후 쿼리 실행 후의 객체와 스냅샷을 비교 by *** 영속성 컨텍스트
        //     -> 변경점이 있다면 update 쿼리를 만들어서 DB에 전송
        // - Test에서는 Rollback이 수행되도록 설정됐으므로 로그에 찍히지 않을수도 -> Test에 @Commit으로 일시적으로 확인 or flush()
    }

    @Override
    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        // find()의 인자 1st - 자료형 / 2nd - PK
        // *** PK 식별자를 통한 단건 조회의 경우 단순하지만, findAll()처럼 조건이 들어간 복잡한 쿼리의 경우에는 jpql을 사용
        return Optional.ofNullable(item);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String jpql = "select i from Item i";
        // Item 엔터티 객체의 별칭 i로부터 i 엔티티 자체를 가져온다?
        // Jpql은 Table이 대상이 아닌 Entity를 대상으로 함

        // 동적 쿼리
        Integer maxPrice = cond.getMaxPrice();
        String itemName = cond.getItemName();

        if (StringUtils.hasText(itemName) || maxPrice != null) {
            jpql += " where";
        }

        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            jpql += " i.itemName like concat('%',:itemName,'%')";
            // * JPA에서는 이름을 통한 binding parameter 사용 가능 - 아래에서 parameter 바인딩하는 로직 존재

            andFlag = true;
            // maxPrice에서 and를 추가할지의 여부를 위해, itemName이 null or not의 상태 확인 가능
        }

        if (maxPrice != null) {
            if (andFlag) {
                jpql += " and";
            }

            jpql += " i.price <= :maxPrice";
        }

        log.info("jpql = {}", jpql);

        //query 생성 및 Parameter 바인딩
        TypedQuery<Item> query = em.createQuery(jpql, Item.class); // 쿼리 수행시점 확인하기
        if (StringUtils.hasText(itemName)) {
            query.setParameter("itemName", itemName);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        //query 수행 및 result 받아오기
        List<Item> result =  query.getResultList();
        return result;

    /*  //동적 쿼리가 아닌 일반적인 경우
        List<Item> result = em.createQuery(jpql, Item.class)
                .getResultList();
        // Item.class는 select i... 의 i의 타입에 해당
        return result;
    */

    }
}
