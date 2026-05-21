/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.platform.domain.model;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@DataJpaTest
class AuditableEntityTest {

    @SpringBootApplication
    @SuppressWarnings("unused")
    static class TestApplication {
    }

    @Autowired
    private TestEntityManager entityManager;

    @Entity
    static class TestAuditableEntity extends AuditableEntity {

        @Column(name = "name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    static class TestUuidAuditableEntity extends UuidAuditableEntity {

        @Column(name = "name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void whenPersisted_thenTimestampsAreSet() throws InterruptedException {
        TestAuditableEntity entity = new TestAuditableEntity();
        entity.setName("initial");

        TestAuditableEntity savedEntity = entityManager.persistAndFlush(entity);

        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isEqualTo(savedEntity.getUpdatedAt());

        OffsetDateTime initialCreatedAt = savedEntity.getCreatedAt();
        OffsetDateTime initialUpdatedAt = savedEntity.getUpdatedAt();

        Thread.sleep(50); // Ensure time difference for update

        // Modify a field to trigger @PreUpdate lifecycle
        savedEntity.setName("updated");
        entityManager.persistAndFlush(savedEntity);
        entityManager.clear();

        TestAuditableEntity fetchedEntity = entityManager.find(TestAuditableEntity.class, savedEntity.getId());
        assertThat(fetchedEntity.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(fetchedEntity.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    void whenUuidPersisted_thenTimestampsAreSet() throws InterruptedException {
        TestUuidAuditableEntity entity = new TestUuidAuditableEntity();
        entity.setName("initial uuid");

        TestUuidAuditableEntity savedEntity = entityManager.persistAndFlush(entity);

        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isEqualTo(savedEntity.getUpdatedAt());

        OffsetDateTime initialCreatedAt = savedEntity.getCreatedAt();
        OffsetDateTime initialUpdatedAt = savedEntity.getUpdatedAt();

        Thread.sleep(50); // Ensure time difference for update

        // Modify a field to trigger @PreUpdate lifecycle
        savedEntity.setName("updated uuid");
        entityManager.persistAndFlush(savedEntity);
        entityManager.clear();

        TestUuidAuditableEntity fetchedEntity = entityManager.find(TestUuidAuditableEntity.class, savedEntity.getId());
        assertThat(fetchedEntity.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(fetchedEntity.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
