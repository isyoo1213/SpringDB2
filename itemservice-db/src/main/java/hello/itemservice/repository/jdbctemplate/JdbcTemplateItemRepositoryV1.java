package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate 구현
 */
@Slf4j
public class JdbcTemplateItemRepositoryV1 implements ItemRepository {

    // * JdbcTemplate 사용을 위한 준비
    // 필드에 선언 + 생성자에 dataSource를 활용한 방식으로 JdbcTemplate 생성하는 것이 일반적인 관례 방법
    // + 직접 JdbcTemplate을 Bean등록하고 주입받는 방식도 가능
    private final JdbcTemplate template;

    public JdbcTemplateItemRepositoryV1(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) values (?,?,?)";

        // DB에서 '자동 증가 생성'을 사용할 경우 JdbcTemplate를 사용할 때의 특수한 처리
        // *** H2 DB의 item 테이블의 id PK는 자동 증가 생성인 identity 전략을 사용 -> DB에 insert가 완료된 후에 값을 확인 가능
        // + DB에 sql을 보내는 것 자체는 상관없지만, repository에서 로직 수행 후 save한 객체를 return할 때 담아 줄 id 값이 필요
        // + 순수 JDBC 로직으로도 가능하지만 복잡하므로, * KeyHolder 사용
        // + 이후 SimpleJdbcInsert라는 훨씬 편리한 기능 사용할 예정
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // update() - INSERT, UPDATE, DELETE일 경우
        template.update(connection -> {
            //자동 증가 키 - DB table에 설정된 coulum 명
            PreparedStatement pstmt = connection.prepareStatement(sql, new String[]{"id"});
            pstmt.setString(1, item.getItemName());
            pstmt.setInt(2, item.getPrice());
            pstmt.setInt(3, item.getQuantity());
            return pstmt;
        }, keyHolder);

        // DB에 저장된 id 값 select 및 저장
        long key = keyHolder.getKey().longValue(); //getKey()는 Number형의 Abstract를 반환하므로 Domain에 맞는 값으로 select해오기
        item.setId(key);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=?, price=?, quantity=? where id=?";
        template.update(sql,
                updateParam.getItemName(),
                updateParam.getPrice(),
                updateParam.getQuantity(),
                itemId);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id=?";
        try {
            Item item = template.queryForObject(sql, itemRowMapper(), id);
            // *** queryForObject() - 단건 조회의 경우에 사용하는 JdbcTemplate 인터페이스에 정의된 추상 메서드
            // 1. 결과가 null일 경우 Exception을 throw
            //    -> try 구문 내에서는 return값 자체는 항상 존재한다고 가정하므로 of()사용 + catch 구문에서는 empty()를 반환
            // 2. 결과가 2개 이상일 경우
            //    -> IncorrectResultSizeDataAccessException 예외 발생
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // *** RowMapper는 ResultSet을 객체로 변환해주는 기능
    private RowMapper<Item> itemRowMapper() {

        // *** return되는 람다표현식 또한 JdbcTemplate이 기존의 JDBC에서 while과 cursor를 사용한 루프를 자동으로 적용해주는 것
        // -> rs, rowNum을 루핑하는 것은 JdbcTemplate의 기능 -> 완성적인 로직을 구현했다고 착각하지 말 것
        return ((rs, rowNum) -> {
            Item item = new Item();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            //Cammel과 Snake case간의 변환 지원해주는지의 여부 확인
            item.setPrice(rs.getInt("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        });
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();
        // *** WrapperClass로 선언한 이유
        // 1. Request에서의 Null을 다루는 것의 편리함
        // 2. Request에서 받아온 값을 가공할 때 Null을 다루는 것의 편리함

        String sql = "select id, item_name, price, quantity from item";

        // * 동적 쿼리가 아닐 경우
        // query() -> List<T>를 반환
        // cf) queryForObject() - 단건을 조회해서 처리할 때
        //template.query(sql, itemRowMapper());

        // * 동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where"; }
        boolean andFlag = false;
        List<Object> param = new ArrayList<>();
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',?,'%')";
            param.add(itemName);
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= ?";
            param.add(maxPrice);
        }

        log.info("sql={}", sql);

        //sql에 parameter를 바인딩해야하므로 이를 Array로 넘겨주는 parameter 추가
        return template.query(sql, itemRowMapper(), param.toArray());
    }
}
