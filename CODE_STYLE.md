Design
------

### Code location ###

Define connected pieces together. This is the most important rule of them all.

When you write a command line tool to
list files in a directory and
have code in the `my.company.fs.cli` package and
you need an utility method to format text into columns, do not define such utility method in the
`my.company.fs.cli` package, define it in the `my.company.text` package.

Group classes by function, not by layer.

Bad package structure:

````
my.company.product.models.User
my.company.product.models.Order
my.company.product.controllers.UserController
my.company.product.controllers.OrderController
my.company.product.views.ProfilePage
my.company.product.views.CheckoutPage
````

Better package structure:

````
my.company.product.user.User
my.company.product.user.UserController
my.company.product.user.ProfilePage
my.company.product.order.Order
my.company.product.order.OrderController
my.company.product.order.CheckoutPage
````

If a method reads lots of properties of
the value of the `MyClass1` class, perhaps
it should be a member of `MyClass1` class.

If you are writing a method for the class `Order` that
calculates the total sum of the order
by summing up all the `OrderLine` values that
are part of an `Order` and
you need to multiply price by quantity for each order line, then
move this multiplication to the `OrderLine` class itself.

Bad code:

````java
    class Order {
        private List<OrderLine> lines;
        long sum() {
            return lines.stream()
                .map(l -> l.price() * l.quantity())
                .sum();
        }
    }
    class OrderLine {
        private long price() {
            // ...
        }
        private long quantity() {
            // ...
        }
    }
````

Good code:

````java
    class Order {
        private List<OrderLine> lines;
        long sum() {
            return lines.stream()
                .map(OrderLine::sum)
                .sum();
        }
    }
    class OrderLine {
        long sum() {
            return price * quantity;
        }
        private long price() {
            // ...
        }
        private long quantity() {
            // ...
        }
    }
````

### Parse, don't validate ###

There are two parts of this maxim.

First, and the most important is that
if you feel a need to write an assertion for one of your parameter,
try to change the type of the parameter, so that
your input is always valid and
you can avoid the check altogether.

Second part of this maxim is the requirement to
make the first part usable.
Do not discard information, try to make it a part of the result.
Do not define a boolean-returning method that checks that
a user with the given name exists in the database, instead
define a method that returns database key of the user.
Do not define a boolean-returning method that checks that
list is not empty, instead
define a method that optionally return
the first element of the list and
the list of remaining elements.
Do not define the method that checks that
the given string is the kind of URL
supported by your application, instead
define the method that parses URL into a data-type that
contains all the interesting parts in readily usable form.

The game here is to eliminate
almost all of the checks in the code and instead
push these checks to the boundaries between components.

Bad code, because `Order.sum` method can silently produce unexpectedly bad resul:

````java
class OrderController {
    Response processRequest(List<Orders> orders) {
        if (orders.isEmpty())
            return Response.ofOrderSum(Decimal.of(0));
        Currency currency = orders.get(0).currency();
        for (Order order: orders) {
            if (!order.currency().equals(currency)) {
                throw new BadRequest("Orders should be of the same currency");
            }
        }
        return Response.ofOrderSum(Order.sum(orders));
    }
}

class Order {
    // Result is silently wrong, when multiple currencies are used
    static Decimal sum(List<Order> orders) {
        return orders.map(Order::value)
                .reduce(Decimal.zero(), Decimal::add);
    }
}
````

Better, because `Order.sum` method is protected from returning bad result, but
still bad, because the same check is performed twice, which means that
information gathered by the check is discarded, instead of being turned into a part of the result.

````java
class OrderController {
    Response processRequest(List<Orders> orders) {
        if (orders.isEmpty())
            return Response.ofOrderSum(Decimal.of(0));
        Currency currency = orders.get(0).currency();
        for (Order order: orders) {
            if (!order.currency().equals(currency)) {
                throw new BadRequest("Orders should be of the same currency");
            }
        }
        return Response.ofOrderSum(Order.sum(orders));
    }
}

class Order {
    static Decimal sum(List<Order> orders) {
        if (orders.isEmpty())
            return Decimal.of(0);
        Currency currency = orders.get(0).currency();
        for (Order order: orders) {
            if (!order.currency().equals(currency)) {
                throw new DifferentCurrencyArgumentException(orders);
            }
        }
        return orders.map(Order::value)
                .reduce(Decimal.zero(), Decimal::add);
    }
}
````

Better code. Now check is performed only once,
`Order.sum` method is changed not to discard information that can be useful later.

````java
class OrderController {
    Response processRequest(List<Orders> orders) {
        Map<Currency, Decimal> evaluatedResult = Order.sum(orders);
        if (evaluatedResult.size() > 1) {
            throw new BadRequest("Orders should be of the same currency");
        }
        Decimal result =
                evaluatedResult.values()
                        .stream()
                        .findFirst()
                        .orElse(Decimal.zero());
        return Response.ofOrderSum(result);
    }
}

class Order {
    static Map<Currency, Decimal> sum(List<Order> orders) {
        return orders.collect(
                Collectors.grouppingBy(
                        Order::currency,
                        Collectors.mapping(
                            Order::value,
                            Collectors.reducing(
                                Decimal.zero(),
                                Decimal::add
                            )
                        )
                )
        );
    }
}
````

### Use explicit generics instead of a uniform representation ###

Use explicit type variables to provide generic functionality, do
not use uniform representation to lower independent types down.
Do not use, string prefixes, like protocol names in URLS, to
pass completely different data through the same gateway, define type variable for this.
Do not steal a part of the type value set to
request additional use cases or
to signal about errors or special cases.

When you lean on generics you can
reuse lots of existing tools of the programming language and
its type-system.
Additionally when you avoid the temptation to implement dynamic type-system yourself, then
the types remain more honest.
When you see a string then you can be sure that
it's just a text and
not some other entity represented as a string.
When you have multiple different entities
all represented as just a string or just an int, then
it means that
you need to treat different strings differently and remember which
string is of which kind.
It's easy to mismatch kinds in such situations and
there is no tooling to lean over.
However there is already an existing tool that
helps distinguish different kinds of values.
It's a type-system.
If you need two values to be treated differently then
make these values have different types.

#### Example: ####

##### Bad code: #####

````java
    class InputSteam {
        int read(); // Negative return value means "end of file"
    }
    class String {
        // number encodes lot's of aspects of beheviour in a single number
        String[] split(String separator, int number);
    }

    // Package names may have internal structure, like group name and artifact name:
    interface SoftwareRepository {
        Set<Integer> getPublishedVersions(String packageName);
        Map<String, Integer> getRequiremens(String packageName, Integer version);
    }
````

##### Better code: #####

````java
    class InputSteam {
        EOFOrChar read();
    }
    class String {
        String[] split(String separator, boolean stripEmptyTrailingParts);
        String[] split(String separator, boolean stripEmptyTrailingParts, int maximumNumberOfFields);
    }
    interface SoftwareRepository<P> {
        Set<Integer> getPublishedVersions(P packageName);
        Map<P, Integer> getRequiremens(P packageName, Integer version);
    }
````

#### Boundaries of type variables ####

Prefer explicit dictionary passing style to variable boundaries.
Subtyping type-inference is undecidable and gives very complicated error messages.
Complex boundaries add a lot of noise to the code and
can result in almost unintelligible error message if something is wrong.
When you explicitly pass dictionaries, then
you explicitly encode the proof of correct typing into you code, which means that
you get more succinct and more targeted error messages, they show you the place
where your proof is wrong.

##### Bad code: #####

````java
    interface Stream<T> {
        static <T extends Comparable<T>> Optional<T> max(Stream<T> stream);
    }
    interface Result<T, E> {
        static <T, E extends ExceptionFactory<X>, X extends Throwable>
                T orElseThrow(Result<T, E> result) throws X;
    }
````

##### Better code: #####

````java
    interface Stream<T> {
        Optional<T> max(Comparator<T> comparator);
    }
    interface Result<T, E> {
        <X extends Throwable> T orElseThrow(Function<E, X> errorToExceptionTransformation) throws X;
    }
````

#### Use wildcards to limit the number of exposed type-variables ####

Type variables can become a significant part of interface and
you should strive to expose not so many type variables.
Expose only those type-variables that affect externally observable behaviour of the class.

##### Bad code: #####

````java
    // `A` type variable doesn't affect externally observable behaviour.
    class DirectoryAggregator<F, A, R> {
        private final Collector<F, A, R> collector;
        private final Function<Path, F> fromFile;

        R aggregate(Path path) {
            return Files.list(path).map(fromFile).collect(collector);
        }
    }

    DirectoryAggregator<F, A, R> aggregator = ...;
    R result = aggregator.aggregate(Path.of("some/path"));
````

##### Better code: #####

````java
    class DirectoryAggregator<F, R> {
        private final Collector<F, ?, R> collector;
        private final Function<Path, F> fromFile;

        R aggregate(Path path) {
            return Files.list(path).map(fromFile).collect(collector);
        }
    }

    DirectoryAggregator<?, R> aggregator = ...;
    R result = aggregator.aggregate(Path.of("some/path"));
````

### Immutability and side-effects ###

Mutability is a very significant complication for the behavior of the class.
Immutable classes can be seen as more complex because
it can be harder to implement (requires more code) immutable class in Java, but
this doesn't matter, only the complexity of the behavior matters.
Classes should be immutable by default until there is a specific reason to be mutable.

Each class can be seen as a manager of some internal state or
otherwise a moderator that gives and moderates access to some external resource.
Some classes, for example `FileSystem` or `File` or `Socket` class, can have all
final fields with immutable data, but these classes give access to inherently stateful environment,
this environment can be seen as a mutable internal state of such classes, even though
there is no mutable field declared in the class.
Classes like these should not be treated as immutable classes.

Each method should have a single effect that is described by the method name.
When method have a return type other then `void`, then
it should preferably be a pure-function or a read-only method.
When the method has the return type `void`, then it can
change the state of its receiver.
A method should avoid changing the state of one of its arguments or
performing its effects through an argument.
This means, for example, that `FileSystem` class can have methods that create or delete files,
because file system state is considered an internal state of the `FileSystem` class, but
a method of another class shouldn't mutate the file system through the `FileSystem` object, when
this object is one of method's arguments.
When a class needs to access the file system through the `FileSystem` object,
this object should be one of the class' fields to be considered part of the class' state.

Bad code:

````java
    class Users {

        // Changes the state of argument:
        static void addUser(List<User> users, User user) {
            users.add(user);
        }
    }
````

Good code:

````java
    class UserCollectionBuilder {
        private List<User> users;

        // Changes the state of receiver:
        void addUser(User user) {
            users.add(user);
        }
    }
````

Bad code:

````java
    class Users {
        static void main(String[] args) {
            renderUser(System.out, User.of("John Doe"));
        }

        // Effect is performed through the argument:
        static void renderUser(PrintStream stream, User user) {
            stream.printf("User[name=%s]", user.name());
        }
    }
````

Good code:

````java
    class Users {
        static void main(String[] args) {
            IOConsumer<User> renderer = createUserRenderer(System.out);
            renderer.accept(User.of("John Doe"));
        }

        // Effect is postponded to be explicit
        static IOConsumer<User> createUserRenderer(PrintStream stream) {
            return user -> stream.printf("User[name=%s]", user.name());
        }
    }
````

### Null hostile code ###

Return `Optional` from methods, never return null.
You can use `Optional` for class fields.
If your method have optional-argument, then create two overloads

 1. one with argument,
 2. another without one.

You should handle null only in two cases:

 1. if there is an explicit sign of null: two method overloads
 2. there may probably be a long time between receiving a possibly null value and actually using it

In the second case, you should not customize your behavior based on the nullness of the argument,
all you may do is just to throw the same `NullPointerException`, that
is going to be thrown anyway sometime later.

In all other cases pretend that `null` doesn't exist.
Do not treat incoming `null` as an empty string or as an empty collection,
accept default behavior, which is throwing an exception.
Simply try to avoid the word "null" in the code.

You should check for `null` explicitly when
there is a long time between receiving a possibly null value and actually using it.
This usually happens in data classes, when a possibly null value is saved as one of the fields.
You may and probably should preemptively check for null in a constructor of a data-class
to prevent "null as a time bomb".

````java
    class UserConfig {
        private final User user;
        private final int numberOfPosts;
        UserConfig(User user, int numberOfPosts) {
            // Early null check:
            this.user = Objects.requireNonNull(user);
            this.numberOfPosts = numberOfPosts;
        }
    }
````

Note that for service-classes in most cases you should not explicitly check for `null`, because
there is usually only one instance of the service class in the running application and
it is trivial to trace `NullPointerException` from object creation
up to the event that causes the error.

#### Example: Handling null ####

Bad code:

````java
void myMethod(String s) {
    // Trying to handle null: don't
    if (s == null || s.isEmpty()) {
        // ...
    }
}
````

Bad code:

````java
void myMethod(String s) {
    // Trying to handle null: don't
    if (Strings.isNullOrEmpty(s)) {
        // ...
    }
}
````

Good code:

````java
void myMethod(String s) {
    if (s.isEmpty()) {
        // ...
    }
}
````

#### Example: Explicit null-argument ####

Bad code:

````java
// Do not use optional method arguments: use overloads instead
void myMethod(String login, Optional<String> readableName) {
    String readableNameString = readableName.orElse(login);
}
````

Bad code:

````java
// No signs that argument can be null
void myMethod(String login, String readableName) {
    if (readableName == null) {
        readableName = login;
    }
}
````

Bad code:

````java
// Do not use nullable annotations: use overloads instead
void myMethod(String login, @Nullable String readableName) {
    if (readableName == null) {
        readableName = login;
    }
}
````

Good code:

````java
void myMethod(String login) {
    myMethod(login, null);
}

void myMethod(String login, String readableName) {
    if (readableName == null) {
        readableName = login;
    }
}
````

Naming
------

### Method names ###

Use **verb** as a first part of the name for all methods that are **not morally pure-functions**.
Use descriptive verbs that tells what effects are performed.

**Do not use verbs** for **morally pure-functions**.
Use single **noun** or adjectives and a **noun** for pure-getter methods, like `List#size()`.

Avoid those names that are ambiguous and the first part of the name can be interpreted as either **verb** or not.

**Morally pure-functions** are those that have no effects other then producing result value and which always produce **morally indistinguishable** results when are supplied with **morally indistinguishable** arguments. Morally indistinguishable values are those that either are the same value in the Java language or can only be distinguished with the use of `Object`-equality (`==` operator) or `System#identityHashCode(Object)` method or tools based on these methods (`IdentityHashMap`).

Unmodifiable objects (those whose fields are all `final`) that use their field values to define `equals` method are morally indistinguishable when all their fields are morally indistinguishable. Mutable values are not morally indistinguishable, even when their `equals` method returns `true`, because two different instances can be distinguished by changing some field of one instance, but not the other, at this point `equals` starts to return `false`. Different instances of mutable-collections (`List`, `Set`, `Collection`) are not morally indistinguishable, even when their `equals` method returns `true`, because two different instances can be distinguished by adding some new elements to one instance, but not the other, at this point `equals` starts to return `false`.

### Factory methods ###

From the above definition it follows that factory methods should be named based on the **moral indistinguishability** of produced values. When a value have a builder-class and a factory-method that produces this builder class, then this factory-method should be named with the verb, because produced builder-objects are easily distinguishable.

Example:

````java
class User {
    // The name starts with the verb, because
    // produced instances of `Builder` are
    // easily distinguishable.
    static Builder createBuilder() {
        return new Builder();
    }

    String name();

    static class Builder {
        User build() {...};
        void setName(String name) {...};
    }
}
````

At the same time when produced values are indistinguishable, then you shouldn't use verbs and should use nouns or even particles.

````java
class Point {
    // Shouldn't use verb in the method name because
    // produced values are morally indistinguishable when
    // arguments are morally indistinguishable.
    static Point of(int x, int y) {
        return new Point(x, y);
    }

    final int x;
    final int y;
}
````

### Stream producing methods ###

For methods returning `Stream`, you may use the verb `stream` in the method name.

Example:

````java
Stream<User> streamUsers() {
    // ...
}
````

### Naming multiple variables that correspond to the same data ###

There are situations when the same piece of data is transformed from one representation to another.
In these situations there may be two or more variables that
all hold conceptually the same piece data,
but have different types.

Do not use type-names as prefixes or suffixes to distinguish different formats.

Name such variables with a noun that describes the conceptual piece of data and
an adjective that describes the process with which
this particular value in this particular representation was obtained.
Verb-based gerund ("ing"-ending) adjectives are usually good for this.
The noun used for variables for the same conceptual piece of data should be the same, but
the adjectives should be different.
Last variable that holds the data in directly usable representation should probably be named with the noun without any adjective.

#### Optional value variable names ####

Above principals about the same data in different formats can be applied to `Optional`-variables.

Name local variables of type `Optional` with the adjective formed by the verb that
describes the process that returns the value.

Bad code:

````java
    Optional<User> optionalUser = findUser(name);
    User user = optionalUser.orElse(admin);
````

Good code:

````java
    Optional<User> foundUser = findUser(name);
    User user = foundUser.orElse(admin);
````

### Conversion method naming ###

When grouping different conversion methods together use *toType* naming scheme,
like `toString` or `toInteger`.
That way you can always derive types of different subexpressions when reading complex expression:

````java
toString(toLocalDateTime(toLocalUser(externalUser).birthDate()))
````

In case when you group different conversion methods as a static methods of the target class
you should use *fromSource* naming scheme, like `fromJson` or `fromExternal`:

````java
class User {
    static User fromJson(Json node) {/* ... */}
    static User fromExternal(ExternalUser external) {/* ... */}
    User(String firstName, String lastName) {
        /* ... */
    }
    /* ... */
}
````

The same rule can be applied if you group such conversion methods into standalone utility-class:

````java
final class Users {
    static User fromJson(Json node) {/* ... */}
    static User fromExternal(ExternalUser external) {/* ... */}
    private Users() {
        throw new UnsupportedOperationException("Utility-classes should not be instantiated");
    }
}
````

When you use *fromType* naming scheme you can still
easily understand the return types of subexpressions of complex expressions
(but you must avoid static imports in this case):

````java
toString(toLocalDateTime(Users.fromExternal(externalUser).birthDate()))
````

Decoration and Aesthetics
-------------------------

### Indentation and curly-braces ###

Use general Kernighan & Ritchie style with hanging braces.
Use 4-space indentation for curly-braced blocks.
Use 8-space indentation for other type of line breaks.

Example:
````
    bla-bla {
        bla;
        bla-bla-bla {
            bla;
            bla;
            bla bla bla
                    bla bla bla
                    bla;
            bla;
        }
        bla bla bla bla
                bla bla bla;
    }
````

Do not use tabs.

### Maximum line width ###

Lines should be no longer then 100 characters.

### Method declaration ###

Start with single line method header and following method body block.

Example:

````java
    public <X, Y> X myMethod(X x, Y y) throws ZException {
        bla;
        bla;
        bla;
    }
````

When method header is too long then break the line before the `throws` keyword.

Example:

````java
    public <X, Y> X myMethod(X x, Y y)
            throws ZException {
        bla;
        bla;
        bla;
    }
````

When the line is still too long, switch to a single parameter per line style.

Example:

````java
    public <X, Y> X myMethod(
            X x,
            Y y
    ) throws ZException {
        bla;
        bla;
        bla;
    }
````

When the line is still too long,
break the line after type-varible declaration and before the return type.

Example:

````java
    public <X, Y> //<br>
    X myMethod(
            X x,
            Y y
    ) throws ZException {
        bla;
        bla;
        bla;
    }
````

Currently `//<br>` comment is required to allow this style to pass current checkstyle rules.

### Method calls ###

When method call can't be fit into a single line, then place each argument on the stand-alone line:

````java
    x.method1(
            "argument",
            5 * y,
            f.translated()
    );
````

Where there is a chain of method calls and the chain can't be fit into a single line, then
place each call on the stand-alone line:

````java
    x.method1(y, 5, "test")
            .method2(f.go(), "test1")
            .method3(z + w)
            .method4();
````

When some call of the method chain can't be fit into a single line, then
place each argument on the stand-alone line:

````java
    x.method1(y, 5, "test")
            .method2(f.go(), "test1")
            .method3(
                    "argument",
                    5 * y,
                    f.translated()
            )
            .method4();
````

When the first method call from the chain can't be fit into a single line, then
break the line between a receiver object and the dot, right before the call itself:

````java
    x
            .method1(
                    "argument",
                    5 * y,
                    f.translated()
            )
            .method2(f.go(), "test1")
            .method3(z + w)
            .method4();
````

You may add a comment to make a hanging receiver expression more readable:

````java
    x //...
            .method1(
                    "argument",
                    5 * y,
                    f.translated()
            )
            .method2(f.go(), "test1")
            .method3(z + w)
            .method4();
````

Alternatively, you may introduce a new one or more local variables to shorten first method call, like:

````java
    var t = f.translated();
    x.method1("argument", 5 * y, t)
            .method2(f.go(), "test1")
            .method3(z + w)
            .method4();
````

or:

````java
    var result1 =
            x.method1(
                    "argument",
                    5 * y,
                    f.translated()
            );
    result1.method2(f.go(), "test1")
            .method3(z + w)
            .method4();
````

### Type variables ###

Type variables should be short, preferably one capital letter.
Instead of a long type-variable name you should document each type variable with javadoc.

When you have 5 or more type-variables used in a single place, you should in most cases switch
to longer names: use 3-4 capital letter abbreviations as type-variable names in this case, like
`KEY`, `OUT`, `ARG`, `VAL`, etc.
In the case of 3-4 letter long names, you still should document type variables in the javadoc.

Bad code:

````java
    static <R, D, C, P, A, O> BuildRecipe<R, D, C, R, A, O> of(
            Class<A> argumentType,
            OutcomeSpec<R, D, P, A, O> outcomeSpec,
            OutcomeExplainer<C, O> outcomeExplainer,
            MemoizationPolicy<O> memoizationPolicy
    ) {
        return new DefaultBuildRuleSpec<>(
                argumentType,
                outcomeSpec,
                outcomeExplainer,
                memoizationPolicy
        );
    }
````

Better code:

````java
    static <RUL, DEP, CTX, RCP, ARG, OUT> //<br>
    BuildRecipe<RUL, DEP, CTX, RCP, ARG, OUT> of(
            Class<ARG> argumentType,
            OutcomeSpec<RUL, DEP, RCP, ARG, OUT> outcomeSpec,
            OutcomeExplainer<CTX, OUT> outcomeExplainer,
            MemoizationPolicy<OUT> memoizationPolicy
    ) {
        return new DefaultBuildRuleSpec<>(
                argumentType,
                outcomeSpec,
                outcomeExplainer,
                memoizationPolicy
        );
    }
````

When the list of type variable-declarations is very long on it's own, then it should be moved to a stand alone line:

````java
class Builder
        <KEY extends Comparable<KEY>, DEP, CTX, RCP, ARG, OUT>
        extends AbstractBuilder {
    static
    <KEY extends Comparable<KEY>, DEP, CTX, RCP, ARG, OUT>
    Builder<KEY, DEP, CTX, RCP, ARG, OUT> createBuilder() {
        // ...
    }
}
````

When the list of type variable declarations is too long for a single line it can be broken:

````java
class Builder
        <KEY extends Comparable<KEY>, DEP, CTX, RCP, REQ,
                RES, ARG, OUT>
        extends AbstractBuilder {
    static
    <KEY extends Comparable<KEY>, DEP, CTX, RCP, REQ, RES,
            ARG, OUT>
    Builder<KEY, DEP, CTX, RCP, REQ, RES, ARG, OUT> createBuilder() {
        // ...
    }
}
````

### Javadoc ###

#### Motivation ####

Write Javadoc to be read as source code.

#### Markup ####

Use simple HTML tags, not valid XHTML:

 * Use a single `<p>` tag on a separate line between paragraphs.
 * Use a single `<li>` tag for items in a list

Example:

````java
    /**
     * Runs foo.
     * <p>
     * This is an example method to illustrate javadoc writing style.
     * We try to include:
     * <ul>
     *     <li>line breaking style;
     *     <li>HTML-tag usage style.
     * </ul>
     * <p>
     * Hope this helps.
     */
````

#### First sentence ####

First sentence should be a stand-alone paragraph to itself followed by the `<p>` tag.

First sentence should use the third person form at the start.
For example, "Gets the foo", "Sets the "bar" or "Consumes the baz".
Avoid the second person form, such as "Get the foo".

#### Line breaks ####

Each new sentence should start on a new line.
Long sentences should be broken into lines to make
each line as readable and as understandable on it's own as possible.
This aids readability as source code, and
simplifies refactoring re-writes of complex Javadoc.

#### Type variables ####

Use indefinite article ('a' or 'an') or plural form when writing javadoc.

Good example:

````java
    /**
     * @param <T> build targets
     * @param <C> a build context available to build targets
     * @param <L> libraries referenced by build targets
     * @param <V> library versions referenced by build targets
     */
    public static class Builder<T extends Comparable<T>, C, L, V extends VersionDesignator<V>> {
        // ...
    }
````

Do not use the word "type" when describing type-variables:

Bad example:

````java
    /**
     * @param <T> type of build targets
     */
    public static class Builder<T extends Comparable<T>> {
        // ...
    }
````

