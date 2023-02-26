package hello.itemservice;

import hello.itemservice.config.*;
import hello.itemservice.repository.ItemRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Import(JdbcTemplateV2Config.class)
//@Import(JdbcTemplateV1Config.class)
//@Import(MemoryConfig.class)
// *** @Import 어노테이션
// - @Configuration 어노테이션이 붙은 클래스간의 계층을 구성하거나 이어줄 때 사용
// 1. @SpringBootApplication 어노테이션은 @Configuration 어노테이션을 포함하지만 ComponentScan에서 제외 대상이 아님
//    -> *** By @ComponentScan 어노테이션 -> 자기 자신은 Bean 등록에서 제외시키지 않음
// 2. ItemServiceApplication의 @ComponentScan 대상은 web에 한정
//    -> @Configuration이 붙은 MemoryConfig.class는, *** 스캔 위치 자체에서 탈락 ( @Configuration 어노테이션 제외가 아님 )
// 3. ItemServiceApplication에서 Bean등록하고자 하는 TestDataInit.class는 ItemRepository에 의존
// 4. ItemReporitory를 주입하기 위해서는 Bean 등록되어있어야 함
//    -> *** @Import 어노테이션을 통해 MemoryConfig.class 또한 Bean등록이 가능한 Configuration으로 작동하도록 설정
// *** 즉, ComponentScan이 적용되는 위치 외에서의 Configuration의 Bean등록을 수동으로 지정해주는 것
@SpringBootApplication(scanBasePackages = "hello.itemservice.web")
public class ItemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}

	@Bean
	@Profile("local")
	// profile 설정이 local일 경우에만 Bean 등록
	// * application.properties에서 spring.profiles.active=local 설정

	// *** Profile?
	//     - 스프링은 로딩 시점에 application.properties의 profile 설정을 읽어서 프로필로 사용
	//     - 로컬/운영환경/테스트실행 등등 환경에 따른 설정을 구성할 때 사용하는 정보
	// *** 즉, 환경에 따라 Bean 등록 및 다양한 설정들의 차별이 필요할 경우에 사용하는 정보
	//     ex) Local에서의 DB, 운영환경에서의 DB 등이 다른 경우 서로 다른 Bean을 등록해야함
	// *** 현재 프로젝트에서는, src/main의 설정과 src/test에서의 프로필 설정을 다르게 진행함
	// * Profile 관련 설정을 하지 않는 경우, default 이름으로 설정됨

	// * 관련 로그
	// 		[           main] h.itemservice.ItemServiceApplication
	// 		: The following 1 profile is active: "local"

	// * Profile 설정을 바꿀 경우, TestDataInit.class가 Bean등록되지 않음
	//   -> Bean등록되지 않으므로 @EventListener가 작동하지 않고
	//    + ApplicationReadyEvent.class의 이벤트를 읽지 못하므로 초기 데이터가 없는 것을 확인 가능
	public TestDataInit testDataInit(ItemRepository itemRepository) {
		return new TestDataInit(itemRepository);
	}

}
