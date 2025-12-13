package com.soyesenna.helixquery;

import com.soyesenna.helixquery.entity.Department;
import com.soyesenna.helixquery.entity.IgnoredFieldModel;
import com.soyesenna.helixquery.entity.User;
import com.soyesenna.helixquery.entity.UserFields;
import com.soyesenna.helixquery.field.NumberField;
import com.soyesenna.helixquery.field.StringField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedFieldsTest {

    @Test
    void generatedFieldsExposeExpectedMetadata() {
        assertEquals("id", UserFields.ID.name());
        assertEquals(Long.class, UserFields.ID.type());
        assertEquals(User.class, UserFields.ID.entityType());
        assertNull(UserFields.ID.relationPath());

        assertEquals("name", UserFields.NAME.name());
        assertEquals(User.class, UserFields.NAME.entityType());
        assertNull(UserFields.NAME.relationPath());

        assertEquals("department", UserFields.DEPARTMENT.$.name());
        assertEquals(Department.class, UserFields.DEPARTMENT.$.targetType());

        assertEquals("department.name", UserFields.DEPARTMENT.NAME.name());
        assertEquals(User.class, UserFields.DEPARTMENT.NAME.entityType());
        assertEquals("department", UserFields.DEPARTMENT.NAME.relationPath());

        assertEquals("address.city", UserFields.ADDRESS_CITY.name());
        assertNull(UserFields.ADDRESS_CITY.relationPath());
    }

    @Test
    void ignoreFieldAnnotationRemovesFieldFromGeneratedConstants() throws Exception {
        Class<?> fieldsClass = Class.forName("com.soyesenna.helixquery.entity.IgnoredFieldModelFields");

        Field idField = fieldsClass.getDeclaredField("ID");
        Object idValue = idField.get(null);
        assertInstanceOf(NumberField.class, idValue);
        assertEquals("id", ((NumberField<?>) idValue).name());
        assertEquals(IgnoredFieldModel.class, ((NumberField<?>) idValue).entityType());

        Field visibleField = fieldsClass.getDeclaredField("VISIBLE");
        Object visibleValue = visibleField.get(null);
        assertInstanceOf(StringField.class, visibleValue);
        assertEquals("visible", ((StringField) visibleValue).name());
        assertEquals(IgnoredFieldModel.class, ((StringField) visibleValue).entityType());

        assertThrows(NoSuchFieldException.class, () -> fieldsClass.getDeclaredField("SECRET"));
    }
}

