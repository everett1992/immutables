/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.mongo;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.OperatorTables;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates mongo find BSON using visitor API.
 */
class MongoQueryVisitor extends AbstractExpressionVisitor<Bson> {

  MongoQueryVisitor() {
    super(e -> { throw new UnsupportedOperationException(); });
  }

  @Override
  public Bson visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = toMongoFieldName(Visitors.toPath(args.get(0)));
      final Object value = Visitors.toConstant(args.get(1)).value();

      return op == Operators.EQUAL ? Filters.eq(field, value) : Filters.ne(field, value);
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = toMongoFieldName(Visitors.toPath(args.get(0)));
      final List<Object> values = Visitors.toConstant(args.get(1)).values();
      Preconditions.checkNotNull(values, "not expected to be null %s", args.get(1));

      return op == Operators.IN ? Filters.in(field, values) : Filters.nin(field, values);
    }


    if (op == Operators.AND || op == Operators.OR) {
      final List<Bson> list = call.arguments().stream()
              .map(a -> a.accept(this))
              .collect(Collectors.toList());

      return op == Operators.AND ? Filters.and(list) : Filters.or(list);
    }
    if (op == Operators.NOT) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      return Filters.not(args.get(0).accept(this));
    }

    if (OperatorTables.COMPARISON.contains(op)) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = toMongoFieldName(Visitors.toPath(args.get(0)));
      final Object value = Visitors.toConstant(args.get(1)).value();

      if (op == Operators.GREATER_THAN) {
        return Filters.gt(field, value);
      } else if (op == Operators.GREATER_THAN_OR_EQUAL) {
        return Filters.gte(field, value);
      } else if (op == Operators.LESS_THAN) {
        return Filters.lt(field, value);
      } else if (op == Operators.LESS_THAN_OR_EQUAL) {
        return Filters.lte(field, value);
      }

      throw new UnsupportedOperationException("Unknown comparison " + call);
    }


    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.operator(), call));
  }

  private static String toMongoFieldName(Path path) {
    Function<AnnotatedElement, String> toStringFn = a -> {
      Objects.requireNonNull(a, "null element");
      if (a.isAnnotationPresent(Criteria.Id.class)) {
        return "_id";
      } else if (a instanceof Member) {
        return ((Member) a).getName();
      } else if (a instanceof Class) {
        return ((Class) a).getSimpleName();
      }

      throw new IllegalArgumentException("Don't know how to name " + a);
    };


    return path.paths().stream().map(toStringFn::apply).collect(Collectors.joining("."));
  }


}