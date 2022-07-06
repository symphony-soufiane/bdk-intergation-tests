package com.symphony.bdk.integrationtest.common;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class Role {
  private final String id;

  public Role(String id) {
    this.id = id;
  }

  public static List<Role> fromRoleIds(List<String> roleAndRoleActionIds) {
    if (roleAndRoleActionIds == null) {
      return new ArrayList();
    } else {
      Map<String, Role> rolesMap = new HashMap();

      for (String roleIdRAId : roleAndRoleActionIds) {
        String[] elems = roleIdRAId.split("\\.", 2);
        Role role = rolesMap.computeIfAbsent(elems[0], k -> new Role(elems[0]));
      }

      return new ArrayList(rolesMap.values());
    }
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      Role role = (Role) o;
      return Objects.equals(this.id, role.id);
    } else {
      return false;
    }
  }
}
