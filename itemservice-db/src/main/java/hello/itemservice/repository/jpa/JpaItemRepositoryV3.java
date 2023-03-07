package hello.itemservice.repository.jpa;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hello.itemservice.domain.Item;
import hello.itemservice.domain.QItem;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static hello.itemservice.domain.QItem.item;

/**
 * Querydsl을 사용
 * - JPAQueryFactory 의존 및 생성 방법
 * - query에 Q클래스 사용하기 + Static Import 활용
 * - BooleanBuilder를 활용해 동적 sql 조건 빌드하기
 * - where() 내부 문법에 활용할 메서드로 더욱 간결하게 Refactoring
 */

// 우선 JPA를 위한 설정 - @Repository : 예외처리 AOP 사용 / @Transactional : JPA의 sql수행은 Transaction 내에서 처리
@Repository
@Transactional
public class JpaItemRepositoryV3 implements ItemRepository {

    private final EntityManager em;

    // * Querydsl의 인터페이스
    //   - cf) EntityManager는 Persistence, 즉 JPA의 인터페이스
    // * Querydsl은 결과적으로 JPA의 jpql을 생성해주는 'builder'역할
    private final JPAQueryFactory query;

    // *** JPAQueryFactory의 생성자 주입 방식
    //     - parameter에 JPAQueryFactory를 받아서 주입하지 않고, EntityManager를 넣어 생성한 인스턴스를 주입
    //     - ex) JDBCTemplate에서 DataSource를 주입받을 때에도 이러한 패턴 사용했었음
    // *** Bean 등록해서 주입하는 방식도 물론 가능함
    public JpaItemRepositoryV3(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }

    // *** 기존 동적 쿼리가 필요하지 않는 부분은 EntityManager을 사용하는 JPA와 동일하게 사용

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
    }

    @Override
    public Optional<Item> findById(Long id) {
        Item item = em.find(Item.class, id);
        return Optional.ofNullable(item);
    }

    // Querydsl 사용
    // * 스프링 예외 추상화는 JPA 계층에서 처리해줌 by @Repository
    public List<Item> findAllOld(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        //QItem item = new QItem("i");
        //variable은 alias의 기능

        // *** 이렇게 직접 생성 제어하지 않고, Q클래스의 static 인스턴스를 가져와서 활용하기 + Static Import
        QItem item = QItem.item;

        // *** BooleanBuilder를 활용한 동적쿼리의 조건 빌드하기
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(itemName)) {
            builder.and(item.itemName.like("%" + itemName + "%"));
        }
        if (maxPrice != null) {
            builder.and(item.price.loe(maxPrice));
        }

        List<Item> result = query
                .select(item)
                .from(item)
                .where(builder)
                .fetch();
        return result;
    }

    // *** 이제 Builder를 통해 동적 쿼리 조건을 생성해주는 것이 아닌, where() 내부에서 바로 적용되는 메서드를 활요하는 문법 사용
    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

/*
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(itemName)) {
            builder.and(item.itemName.like("%" + itemName + "%"));
        }
        if (maxPrice != null) {
            builder.and(item.price.loe(maxPrice));
        }
*/

        // inline으로 return 합쳐서 더욱 간단하게
        // Static Import 사용
        // *** where() 내부에서는 ','를 통해 and 조건 형성 + null은 무시하는 문법을 통해 동적 조건 간결하게 작성 가능
        //     + *** Method화 시킨 조건 로직이 모듈화되므로 재사용 가능함
        return query
                .select(item)
                .from(item)
                .where(likeItemName(itemName), maxPrice(maxPrice))
                .fetch();
    }

    private BooleanExpression likeItemName(String itemName) {
        if (StringUtils.hasText(itemName)) {
            // * where() 내에 바로 return할 수 있는 문법을 사용하므로 따로 builder로 만들어 줄 필요가 없음
            return item.itemName.like("%" + itemName + "%");
        }
        return null;
    }

    private BooleanExpression maxPrice(Integer maxPrice) {
        if (maxPrice != null) {
            return item.price.loe(maxPrice);
        }
        return null;
    }

}
