package hello.itemservice.repository.mybatis;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MyBatisItemRepository implements ItemRepository {
    // * 대부분이 매퍼인터페이스에 위임해 내부적으로 동작하는 구조

    //매퍼 인터페이스 주입
    // *** Proxy를 통해 구현체를 만들어 xml 호출 등에 사용
    private final ItemMapper itemMapper;

    @Override
    public Item save(Item item) {

        //ItemMapper에 어떤 인스턴스가 주입되어있는지 확인하기 위한 로그
        log.info("itemMapper class = {}", itemMapper.getClass());
        //실제 로그
        //h.i.r.mybatis.MyBatisItemRepository
        //: itemMapper class = class com.sun.proxy.$Proxy66
        // -> ItemMapper를 상속받은 객체를 인스턴스화

        itemMapper.save(item);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        itemMapper.update(itemId, updateParam);
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemMapper.findById(id);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        return itemMapper.findAll(cond);
    }
}
