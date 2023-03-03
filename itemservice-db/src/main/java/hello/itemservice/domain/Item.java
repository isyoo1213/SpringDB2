package hello.itemservice.domain;

import lombok.Data;

import javax.persistence.*;


@Data
@Entity // JPA가 사용하는 객체임을 명시 -> 해당 어노테이션이 필수적으로 있어야 JPA가 인식
@Table(name = "item")
public class Item {

    @Id //Table의 PK와 매핑
    @GeneratedValue(strategy = GenerationType.IDENTITY) //PK생성 값을 DB에서 생성하는 Identity전략을 사용
    private Long id;

    @Column(name = "item_name", length = 10)
    // length - JPA 매핑정보로 DDL(create.. talbe)또한 가능하며, 그 때의 컬럼의 길이값
    // ex) (varchar 10)
    // *** SpringBoot와 통합해서 해당 어노테이션 사용 시, Camel -> Snake로 자동 변환해줌
    // -> 위 어노테이션의 name 속성을 삭제해도 무방
    private String itemName;

    private Integer price;
    private Integer quantity;

    // *** JPA는 public/protected의 기본 생성자가 필수 by JPA 스펙 for Proxy생성 등등
    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}
