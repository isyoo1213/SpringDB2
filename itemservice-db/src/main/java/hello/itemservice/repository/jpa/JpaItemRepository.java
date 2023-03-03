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
@Transactional
// *** JPA의 모든 데이터 변경(조회는 transaction없이 가능)은 Transaction 내에서 이루어짐
// + *** 일반적으로는 비즈니스 로직이 시작되는 계층에서 transaction을 걸어주는 것이 맞음(주로 Service)
public class JpaItemRepository implements ItemRepository {

    // *** JPA의 실질적인 로직을 수행해주는 의존성 EntityManager
    //    - Spring과 통합된 환경이므로 관련 설정을 모두 잡아줌
    //    - 그렇지 않을 경우 EntityManagerFactory, JpaTransactionManager, DataSource 설정 등을 해주어야함
    // * SpringBoot의 자동설정은 JpaBaseConfiguration을 참고
    private final EntityManager em;

    public JpaItemRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Item save(Item item) {
        em.persist(item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = em.find(Item.class, itemId);
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
        // *** update() 관련 로직을 수행하지 않음
        // - Transaction이 Commit되는 시점에 update 쿼리를 만들어서 DB에 전송
        //   -> Test에서는 Rollback이 수행되도록 설정됐으므로 로그에 찍히지 않을수도
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
