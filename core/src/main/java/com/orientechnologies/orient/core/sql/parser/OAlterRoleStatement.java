/* Generated By:JJTree: Do not edit this line. OAlterRoleStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityPolicyImpl;
import com.orientechnologies.orient.core.sql.executor.OInternalResultSet;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OAlterRoleStatement extends OSimpleExecStatement {

  static class Op {

    protected static int TYPE_ADD = 0;
    protected static int TYPE_REMOVE = 1;

    Op(int type, OSecurityResourceSegment resource, OIdentifier policyName) {
      this.type = type;
      this.resource = resource;
      this.policyName = policyName;
    }

    protected final int type;
    protected final OSecurityResourceSegment resource;
    protected final OIdentifier policyName;
  }

  protected OIdentifier name;
  protected List<Op> operations = new ArrayList<>();

  public OAlterRoleStatement(int id) {
    super(id);
  }

  public OAlterRoleStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OResultSet executeSimple(OCommandContext ctx) {
    OInternalResultSet rs = new OInternalResultSet();
    ODatabaseSession db = (ODatabaseSession) ctx.getDatabase();
    OSecurityInternal security = ((ODatabaseInternal) db).getSharedContext().getSecurity();

    ORole role = db.getMetadata().getSecurity().getRole(name.getStringValue());
    if (role == null) {
      throw new OCommandExecutionException("role not found: " + name.getStringValue());
    }
    for (Op op : operations) {
      OResultInternal result = new OResultInternal();
      result.setProperty("operation", "alter role");
      result.setProperty("name", name.getStringValue());
      result.setProperty("resource", op.resource.toString());
      if (op.type == Op.TYPE_ADD) {
        OSecurityPolicyImpl policy = security.getSecurityPolicy(db, op.policyName.getStringValue());
        result.setProperty("operation", "ADD POLICY");
        result.setProperty("policyName", op.policyName.getStringValue());
        try {
          security.setSecurityPolicy(db, role, op.resource.toString(), policy);
          result.setProperty("result", "OK");
        } catch (Exception e) {
          result.setProperty("result", "failure");
        }
      } else {
        result.setProperty("operation", "REMOVE POLICY");
        try {
          security.removeSecurityPolicy(db, role, op.resource.toString());
          result.setProperty("result", "OK");
        } catch (Exception e) {
          result.setProperty("result", "failure");
        }
      }
      rs.add(result);
    }
    return rs;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER ROLE ");
    name.toString(params, builder);

    for (Op operation : operations) {
      if (operation.type == Op.TYPE_ADD) {
        builder.append(" SET POLICY ");
        operation.policyName.toString(params, builder);
        builder.append(" ON ");
        operation.resource.toString(params, builder);
      } else {
        builder.append(" REMOVE POLICY ON ");
        operation.resource.toString(params, builder);
      }
    }
  }
}
/* JavaCC - OriginalChecksum=1a221cad0dfbc01f66a720300b776def (do not edit this line) */
