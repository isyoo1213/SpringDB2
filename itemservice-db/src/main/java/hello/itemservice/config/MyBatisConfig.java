package hello.itemservice.config;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.mybatis.ItemMapper;
import hello.itemservice.repository.mybatis.MyBatisItemRepository;
import hello.itemservice.service.ItemService;
import hello.itemservice.service.ItemServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class MyBatisConfig {

    // *** MyBatis 라이브러리가 DataSource, TransactionManager등을 매퍼 인스턴스와 내부적으로 자동으로 연결시켜줌
    private final ItemMapper itemMapper;
    // *** 해당 Config에서 ItemMampper를 주입받는 과정 검수해보기 - 인터페이스인데 어떻게 구현체를 주입받을까?
    //     1. MyBatis 스프링 연동 모듈이 @Mapper 인터페이스를 조회
    //     2. 매퍼 인스턴스의 '동적 프록시 객체 생성' like AOP
    //     3. 내부적으로 인터페이스의 메서드들을 연결하고 생성하는 로직 수행 ex) xml의 설정을 읽어들여 실질적으로 로직 수행할 수 있도록
    //     4. 동적 프록시 객체를 Bean으로 등록
    // -> MyBatis 스프링 연동 모델은 MyBatisAutoConfiguration 클래스 참고

    // *** MyBatis가 생성한 매퍼 구현체는 Exception 변환 처리까지 수행
    //     -> MyBatis에서 발생한 Exception을 스프링 예외 추상화인 DataAccessException에 맞게 변환해서 반환

    @Bean
    public ItemService itemService() {
        return new ItemServiceV1(itemRepository());
    }

    @Bean
    public ItemRepository itemRepository() {
        return new MyBatisItemRepository(itemMapper);
    }
}
