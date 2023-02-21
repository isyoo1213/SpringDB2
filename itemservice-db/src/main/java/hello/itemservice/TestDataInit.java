package hello.itemservice;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@RequiredArgsConstructor
public class TestDataInit {

    private final ItemRepository itemRepository;

    /**
     * 확인용 초기 데이터 추가
     */
    @EventListener(ApplicationReadyEvent.class)

    // *** @EventListener
    // Spring 컨테이너가 'AOP'를 포함한 초기화를 끝내고 실행 준비가 되었을 때 ApplicationReadyEvent 발생
    // -> 이를 캐치하고 init() 메서드 실행
    // *** Container가 준비되었을 때의 로그
    //     [           main] h.itemservice.ItemServiceApplication
    //     : Started ItemServiceApplication in 4.122 seconds (JVM running for 4.777)
    // + Bean으로 등록되어있어야 Container를 띄우면서 인식 가능

    // *** cf) @PostConstruct 를 사용할 경우
    // -> AOP가 처리되지 않은 상태에서 호출될 수 있음 -> @Transactional과 관련된 문제 발생 가능
    public void initData() {
        log.info("test data init");
        itemRepository.save(new Item("itemA", 10000, 10));
        itemRepository.save(new Item("itemB", 20000, 20));
    }

}
