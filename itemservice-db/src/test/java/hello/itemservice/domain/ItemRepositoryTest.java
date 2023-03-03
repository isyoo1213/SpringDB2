package hello.itemservice.domain;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import hello.itemservice.repository.memory.MemoryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// * @SpringBootTest
// - 해당 어노테이션은 @SpringBootApplication 어노테이션이 붙은 클래스를 탐색
// - 해당 클래스의 Config를 설정으로 사용
// - 이전까진 @SpringBootApplication가 붙은 어플리케이션 클래스에서 MemoryRepository를 @Import()하고 있었지만
//   현재는 JdbcTemplateRepositoryV3를 @Import()하고 있으므로 V3 레포지토리를 주입받아 사용함

@Slf4j
@Transactional
// *** Test 메서드나 클래스에 붙어있을 경우 로직 성공시 Commit이 아닌 Rollback 수행
// - 메서드 단위의 Test에도 적용 가능
// *** 트랜잭션 전파
// - repository에서 사용하는 JdbcTemplate 등 con을 사용하는 곳 모두 트랜잭션을 시작한 동기화 매니저 내부의 con을 공유
// - *** Service나 Repository 등에 적용된 @Transactional에서도 트랜잭션, con이 전파됨
@SpringBootTest
class ItemRepositoryTest {
    // *** 현재 Test에서는 repository의 구현체가 아닌 Interface 자료형으로 Test 진행
    // -> 상황에 따라서 구현체에 의존하는 Test로 진행될 수 있지만, 추상화를 통해 범용성있는 Test 설계가 우선

    @Autowired
    ItemRepository itemRepository;

/*
    // * Transaction 사용을 위한 의존성
    // + * Datasource와 transactionManager는 Spring이 자동으로 Bean 등록해줌
    @Autowired
    PlatformTransactionManager transactionManager;

    TransactionStatus status; // Rollback 설정에 필요하므로 필드로 선언만 하고 이후에 생성해주는 방식

    // * 모든 Test 실행 전에 status를 생성하기 -> Transaction 시작해버리기
    @BeforeEach
    void beforeEach() {
        // transactionManager에서 status 가져와버리기
        // *** transaction을 시작하면 얻는 이점
        // 굳이 commit하지 않아도 나의 session에서는 수행된 DB데이터들을 조회하는 것은 가능
        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    }
*/

    @AfterEach
    void afterEach() {
        //MemoryItemRepository 의 경우 제한적으로 사용
        // * ItemRepository 인터페이스 자체에는 clearStore() 메서드 없음 + MemoryItemRepository만 가지고 있는 메서드
        //   -> itemRepository 인스턴스 자체는 ItemRepository 자료형이므로 다운캐스팅 로직이 필요
        // * 실제 DB를 사용하는 경우에는 테스트 끝나는 시점에 transaction을 rollback해서 데이터 초기화할 예정
        if (itemRepository instanceof MemoryItemRepository) {
            ((MemoryItemRepository) itemRepository).clearStore();
        }

        //Transaction 롤백
        // * 현재 롤백 로그에는 JdbcTranscationManager를 설정했지만 실질적으로는 DatasourceTransactionManager를 상속
        //transactionManager.rollback(status);
    }

    // *** 트랜잭션 내에서 Commit()을 수행하고 싶을 경우
    //@Commit
    //@Rollback(value = false)
    @Test
    void save() {
        //given
        Item item = new Item("itemA", 10000, 10);

        //when
        Item savedItem = itemRepository.save(item);
        // *** 실제 주입된 repository의 jdbcInsert는 트랜잭션 동기화 매니저 내에 저장된 con을 사용함
        // -> 동일한 트랜잭션 내에서는 동일한 con으로 로직 수행

        //then
        Item findItem = itemRepository.findById(item.getId()).get();
        assertThat(findItem).isEqualTo(savedItem);
    }

    @Test
    void updateItem() {
        //given
        Item item = new Item("item1", 10000, 10);
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getId();

        //when
        ItemUpdateDto updateParam = new ItemUpdateDto("item2", 20000, 30);
        itemRepository.update(itemId, updateParam);

        //then
        Item findItem = itemRepository.findById(itemId).get();
        assertThat(findItem.getItemName()).isEqualTo(updateParam.getItemName());
        assertThat(findItem.getPrice()).isEqualTo(updateParam.getPrice());
        assertThat(findItem.getQuantity()).isEqualTo(updateParam.getQuantity());
    }

    @Test
    void findItems() {
        //JPA 활용 시, @Repository 어노테이션을 사용 시 repository의 Proxy가 생성되는지 확인하는 로그
        // * @Transactional 또한 Proxy를 생성하므로 주석처리해야 정확하게 파악 가능
        log.info("Repository = {}", itemRepository.getClass());
        //실제 로그
        //Repository = class hello.itemservice.repository.jpa.JpaItemRepository$$EnhancerBySpringCGLIB$$d0843546

        //given
        Item item1 = new Item("itemA-1", 10000, 10);
        Item item2 = new Item("itemA-2", 20000, 20);
        Item item3 = new Item("itemB-1", 30000, 30);

        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        //둘 다 없음 검증
        test(null, null, item1, item2, item3);
        test("", null, item1, item2, item3);

        //itemName 검증
        test("itemA", null, item1, item2);
        test("temA", null, item1, item2);
        test("itemB", null, item3);

        //maxPrice 검증
        test(null, 10000, item1);

        //둘 다 있음 검증
        test("itemA", 10000, item1);
    }

    void test(String itemName, Integer maxPrice, Item... items) { // *** 가변인수 문법 확인하기
        List<Item> result = itemRepository.findAll(new ItemSearchCond(itemName, maxPrice));
        assertThat(result).containsExactly(items);
        // * containsExactly() - 순서 또한 정확하게 맞아야 함
    }
}
