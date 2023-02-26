package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NamedParameterJdbcTemplate -> key + values 형태의 Parameter Binding 객체로 바인딩하기
 *   - SqlParameterSource 인터페이스로 binding할 Parameter를 구현체를 통해 객체화
 *     구현체 방법 1. BeanPropertySqlParameterSource -> 객체에서 자동으로 parameter 추출하기
 *     구현체 방법 2. MapSqlParameterSource -> 객체에서 수동으로 parameter 추출하기
 *   방법 3. Map 방식으로 binding 객체 생성
 *   - BeanPropertyRowMapper -> 기존의 JdbcTemplate이 RowMapper로 루핑해서 구성해주는 로직을 간단하게 구현
 */
@Slf4j
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    // * 이제 sql의 parameter 바인딩을 순서가 아닌 이름 기반으로 바인딩할 예정
    //private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate template;

    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        //this.template = new JdbcTemplate(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);

    }

    @Override
    public Item save(Item item) {
        //String sql = "insert into item(item_name, price, quantity) values (?,?,?)";
        String sql = "insert into item(item_name, price, quantity) " +
                "values (:itemName ,:price, :quantity)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 방법 1.
        // * BeanPropertySqlPamaterSource() + SqlParameterSource(여러 자료형으로 래핑 가능)
        //   -> Repository의 CRUD 메서드로 전달된 객체에서 Binding할 parameter 정보를 자동으로 추출 by 자바빈 프로퍼티 규약
        //      ex) getItemName() -> key -> itemName / value -> 상품명 값
        // * 현재 Item은 @Data를 활용하고있으므로 getter/setter를 인식해서 필드명들을 가져옴
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        // 추출한 SqlParameterSource 자료형의 param을 전달해주기만 하면, sql에 매핑해줌
        // + KeyHolder를 위한 처리 또한 parameter를 Binding하는 단계에서 신경쓸 필요가 없어짐
        template.update(sql, param, keyHolder);

    /*  //기존의 update() 메서드와 parameter 전달
        template.update(connection -> {
            PreparedStatement pstmt = connection.prepareStatement(sql, new String[]{"id"});
            pstmt.setString(1, item.getItemName());
            pstmt.setInt(2, item.getPrice());
            pstmt.setInt(3, item.getQuantity());
            return pstmt;
        }, keyHolder);
    */

        // DB에 저장된 id 값 select 및 저장
        long key = keyHolder.getKey().longValue(); //getKey()는 Number형의 Abstract를 반환하므로 Domain에 맞는 값으로 select해오기
        item.setId(key);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        //String sql = "update item set item_name=?, price=?, quantity=? where id=?";
        String sql = "update item set " +
                "item_name=:itemName, price=:price, quantity=:quantity " +
                "where id=:id";

        // 방법 2.
        // MapSqlParameterSource + SqlParameterSource
        // 메서드 체인을 통해 편리한 사용 가능
        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);
        // *** "id"의 경우, ItemUpdateDto가 아닌 Item의 필드이므로, BeanPropertySqlParameterSource를 적용하지 못하는 경우에 해당

        template.update(sql, param);

    /*  //기존의 update()
        template.update(sql,
                updateParam.getItemName(),
                updateParam.getPrice(),
                updateParam.getQuantity(),
                itemId);
    */
    }

    @Override
    public Optional<Item> findById(Long id) {
        //String sql = "select id, item_name, price, quantity from item where id=?";
        String sql = "select id, item_name, price, quantity from item where id=:id";

        try {
            //Item item = template.queryForObject(sql, itemRowMapper(), id);

            // 방법 3. Map 사용 - JAVA 순수 문법
            Map<String, Object> param = Map.of("id", id);
            // new HashMap<>()으로 직접 생성해 사용하는 것도 무방

            // *** Map.of() / Map.ofEntries()
            // JAVA 9이상부터 Map 자료구조를 초기화할 수 있음
            // 주의점
            // 1. Map.of()의 경우, parameter의 개수를 10개만큼 overloading하므로 이를 초과할 경우 오류 발생
            // 2. of()/ofEntries() 모두 immutableCollections를 반환 -> 초기화 이후 put(), remove()등으로 수정 불가능

            Item item = template.queryForObject(sql, param, itemRowMapper());

            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // *** RowMapper는 ResultSet을 객체로 변환해주는 기능
    private RowMapper<Item> itemRowMapper() {

    /*  // 기존 JdbcTemplate의 RowMapper로의 루핑 로직
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
    */

        // * BeanPropertyRowMapper
        //   - ResultSet의 결과를 받아서 자바빈 규약에 맞게 메서드 호출 및 데이터를 변환해줌
        //   - rs, cursor등의 반복과 매핑을 설정한 클래스에 맞게 모두 구성해줌
        // * Camel case 변환 지원
        //   - DB에서 가져온 coulum과 Domain/DTO에서의 변수명이 다른 경우 -> 일반적으로 sql의 as 별칭을 사용해서 해결
        //   * 관례의 불일치
        //   - RDBMS는 주로 snake case를 사용하므로, 이를 자바의 camel case로 변환해주는 기능을 기본적으로 제공
        // -> 즉, column 네임과 객체의 필드명이 완전히 다를 경우에만 sql as 별칭을 손봐주면 해결
        return BeanPropertyRowMapper.newInstance(Item.class);
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        //String sql = "select id, item_name, price, quantity from item";

        // 방법 1.
        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";

        // * 동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where"; }
        boolean andFlag = false;
        //List<Object> param = new ArrayList<>();
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            //param.add(itemName);
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
            //param.add(maxPrice);
        }

        log.info("sql={}", sql);

        //sql에 parameter를 바인딩해야하므로 이를 Array로 넘겨주는 parameter 추가

        //return template.query(sql, itemRowMapper(), param.toArray());
        return template.query(sql, param, itemRowMapper());

    }
}
