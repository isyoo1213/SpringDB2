package hello.itemservice.repository.jpa;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * * SpringDataJpa를 활용한 Repository는 ItemRepository 인터페이스를 구현하고 있지 않음
 *    - Service 계층에서 Repository를 주입할 경우, ItemRepository가 아닌 SPJ Repository를 추가적으로 의존성 주입해주어야함
 *    -> Service 계층이 새로운 의존관계를 형성하며 메서드의 내부로직을 SPJ에 맞게 전부 새로 짜주어야함
 * * 해결방법
 *    - Service 계층에서 곧장 SPJ 인터페이스를 의존받지 않고, 기존 ItemRepository를 SPJ로 구현하는 Repository를 추가
 *    -> Service 계층 입장에서는 기존대로 ItemRepository를 주입받으므로, SPJ를 위한 추가적인 의존관계 형성을 피할 수 있음
 * * 실제로는 해당 Repository의 Proxy가 실제 구현체가 되어 Bean등록되고, Runtime 객체 의존관계를 형성
 *    - Runtime 객체 의존관계
 *    -> itemService (인터페이스) -> jpaItemRepositoryV2 (구현체) -> (의존)<<Proxy>>springDataJpaItemRepository (Proxy객체)
 * * 즉, JpaItemRepository가 어댑터와 같은 역할을 함으로써 Service계층의 코드를 건드리지 않아도 됨
 */

@Slf4j
@Repository
// * SDJ에서의 예외변환
//  - *** SDJ는 Persistence가 아닌 Springframework의 하위 기술이므로 모두 Spring 내에서 처리 가능
//  - *** JPA에서 @Repository 어노테이션이 제공하는 예외변환 AOP Proxy가 필수적이지 않음
//  - 만약, spring에서 넘어온 예외변환된 예외가 @Repository의 AOP로 넘어와도 이미 변환된 예외이므로 무시하고 넘어감
@Transactional
@RequiredArgsConstructor
public class JpaItemRepositoryV2 implements ItemRepository {

    private final SpringDataJpaItemRepository repository; // @RequiredArgsConstructor로 바로 주입되도록 만듦


    @Override
    public Item save(Item item) {
        return repository.save(item);
        // *** 기본적인 CRUD 메서드는 JPA가 아닌 DATA의 CrudRepository 인터페이스에 규격
        // *** JpaRepository 인터페이스는 상속관계를 올라가다보면 이 DATA의 CrudRepository를 상속하고 있음
        // -> 실질적인 구현체인 '클래스'는 SimpleJpaRepository
        // -> 결국 EntityManager인 em.persist() 메서드가 호출됨
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        Item findItem = repository.findById(itemId).orElseThrow();
        //findById()의 반환형이 Optional이므로 orElseThrow()처리

        //JPA에서의 update()는 메서드를 호출하는 것이 아닌 값만 세팅해주면 된다 - Commit 실행 시점에 DB에 반영
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
    }

    @Override
    public Optional<Item> findById(Long id) {
        return repository.findById(id);
        // CrudRepository의 구현체인 SimpleJpaRepository의 findById()메서드를 사용
        // + 반환타입 또한 Optional<T> -> 바로 리턴가능
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        // 지금은 그냥 SDJ가 아닌 JPA에서의 처리처럼만 하고 이후에 SDJ를 활용한 깔끔한 처리

        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        if (StringUtils.hasText(itemName) && maxPrice != null) {
            //return repository.findByItemNameLikeAndPriceLessThanEqual(itemName, maxPrice);
            return repository.findItems("%" + itemName + "%", maxPrice);
        } else if (StringUtils.hasText(itemName)) {
            return repository.findByItemNameLike("%" + itemName + "%");
        } else if (maxPrice != null) {
            return repository.findByPriceLessThanEqual(maxPrice);
        } else {
            return repository.findAll();
            // findAll()은 JpaRepository 인터페이스의 메서드
        }
    }
}
