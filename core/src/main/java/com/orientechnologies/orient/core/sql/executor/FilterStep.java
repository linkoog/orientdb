package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.resultset.OFilterResultSet;
import com.orientechnologies.orient.core.sql.executor.resultset.OLimitedResultSet;
import com.orientechnologies.orient.core.sql.parser.OWhereClause;

/** Created by luigidellaquila on 12/07/16. */
public class FilterStep extends AbstractExecutionStep {
  private final long timeoutMillis;
  private OWhereClause whereClause;

  private OResultSet prevResult = null;

  private long cost;

  public FilterStep(
      OWhereClause whereClause, OCommandContext ctx, long timeoutMillis, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.whereClause = whereClause;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public OResultSet syncPull(OCommandContext ctx, int nRecords) throws OTimeoutException {
    if (!prev.isPresent()) {
      throw new IllegalStateException("filter step requires a previous step");
    }

    return new OLimitedResultSet(
        new OFilterResultSet(() -> fetchNext(ctx, nRecords), (result) -> filterMap(ctx, result)),
        nRecords);
  }

  private OResult filterMap(OCommandContext ctx, OResult result) {
    long timeoutBegin = System.currentTimeMillis();
    long begin = profilingEnabled ? System.nanoTime() : 0;
    try {
      if (whereClause.matchesFilters(result, ctx)) {
        return result;
      }
    } finally {
      if (profilingEnabled) {
        cost += (System.nanoTime() - begin);
      }
    }
    if (timeoutMillis > 0 && timeoutBegin + timeoutMillis < System.currentTimeMillis()) {
      sendTimeout();
    }
    return null;
  }

  private OResultSet fetchNext(OCommandContext ctx, int nRecords) {
    OExecutionStepInternal prevStep = prev.get();
    if (prevResult == null) {
      prevResult = prevStep.syncPull(ctx, nRecords);
    } else if (!prevResult.hasNext()) {
      prevResult = prevStep.syncPull(ctx, nRecords);
    }
    return prevResult;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    StringBuilder result = new StringBuilder();
    result.append(OExecutionStepInternal.getIndent(depth, indent) + "+ FILTER ITEMS WHERE ");
    if (profilingEnabled) {
      result.append(" (" + getCostFormatted() + ")");
    }
    result.append("\n");
    result.append(OExecutionStepInternal.getIndent(depth, indent));
    result.append("  ");
    result.append(whereClause.toString());
    return result.toString();
  }

  @Override
  public OResult serialize() {
    OResultInternal result = OExecutionStepInternal.basicSerialize(this);
    if (whereClause != null) {
      result.setProperty("whereClause", whereClause.serialize());
    }

    return result;
  }

  @Override
  public void deserialize(OResult fromResult) {
    try {
      OExecutionStepInternal.basicDeserialize(fromResult, this);
      whereClause = new OWhereClause(-1);
      whereClause.deserialize(fromResult.getProperty("whereClause"));
    } catch (Exception e) {
      throw OException.wrapException(new OCommandExecutionException(""), e);
    }
  }

  @Override
  public long getCost() {
    return cost;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }

  @Override
  public OExecutionStep copy(OCommandContext ctx) {
    return new FilterStep(this.whereClause.copy(), ctx, timeoutMillis, profilingEnabled);
  }
}
