package hello.itemservice.repository;

import hello.itemservice.domain.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    // *** ItemUpdateDto 와 ItemSearchCond 클래스의 위치
    // -> 현재 구성은, Controller -> Service -> Repository 로 호출하는 관계
    // -> Service에서 두 클래스를 사용하는 것도, 결국 Repository에서 이를 사용하기 위해 전달해주는 수단
    // -> Repository 계층에 위치하는 것이 합리적
    // * 만약 Service 단에서만 사용되고 Repository까지 전달되지 않는 DTO라면 Service에 위치하는 것이 합리적
    // * 추가적으로 순환참조에 관한 내용 공부 필요

    Item save(Item item);

    void update(Long itemId, ItemUpdateDto updateParam);

    Optional<Item> findById(Long id);

    List<Item> findAll(ItemSearchCond cond);

}
