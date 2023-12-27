Grumpinessy
===========

![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.sviperll/grumpinessy/badge.png?style=flat)

This project consists of several parts:

 1. [my personal Checkstyle configuration](src/main/resources/com/github/sviperll/grumpinessy/checkstyle.xml)
 2. implementation of custom Checkstyle checks that are used in my checkstyle configuration
 3. my personal Code Style Guide

Code Style Guide
----------------

My style guide is a little eclectic.
I mostly try to avoid those things that I feel are pretty well understood and wide-spread and
instead I focus mostly on those things that I do differently or
which I feel not yet caught up in the mainstream.

Read the guide [here](CODE_STYLE.md)

Using Checkstyle configuration
------------------------------

To apply checkstyle configuration to your project,
you need to configure `maven-checkstyle-plugin` to use grumpinessy configuration.
To do this, you should configure `maven-checkstyle-plugin` to depend on `grumpinessy` artifact and
to reference `checkstyle.xml` configuration file in the `com/github/sviperll/grumpinessy/`
namespace. This looks like the following:

````xml
    <!-- ... -->
    <build>
        <plugins>
            <!-- ... -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version><!-- ... --></version>
                <dependencies>
                    <!-- ... -->
                    <dependency>
                        <groupId>com.github.sviperll</groupId>
                        <artifactId>grumpinessy</artifactId>
                        <version><!-- ... --></version>
                    </dependency>
                </dependencies>
                <configuration>
                    <configLocation>com/github/sviperll/grumpinessy/checkstyle.xml</configLocation>
                    <!-- ... -->
                </configuration>
            </plugin>
            <!-- ... -->
        </plugins>
    </build>
    <!-- ... -->
````

Full configuration to enable checkstyle checks for your project may look like this:

````xml
    <!-- ... -->
    <build>
        <plugins>
            <!-- ... -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.1.2</version>
                <dependencies>
                    <dependency>
                        <groupId>com.puppycrawl.tools</groupId>
                        <artifactId>checkstyle</artifactId>
                        <version>10.12.6</version>
                    </dependency>
                    <dependency>
                        <groupId>com.github.sviperll</groupId>
                        <artifactId>grumpinessy</artifactId>
                        <version>0.5</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <configLocation>com/github/sviperll/grumpinessy/checkstyle.xml</configLocation>
                    <includeTestSourceDirectory>true</includeTestSourceDirectory>
                    <includeResources>false</includeResources>
                    <includeTestResources>false</includeTestResources>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- ... -->
        </plugins>
    </build>
    <!-- ... -->
````

Individual Checkstyle Checks
----------------------------

### com.github.sviperll.grumpinessy.NoImportsOfHigherPackagesCheck ###

Checks that deeper package do not import enclosing package.
As a convention we assume that enclosed packages are implementation details of enclosing packages.
And their definition should be self-contained and
the relationship between enclosing and enclosed package should be of a single direction.
Enclosing package should depend on the enclosed package, but opposite is forbidden.

Ok (Enclosing package `x.y` references enclosed pacakge `x.y.z`):

````java
package x.y;

import x.y.z.Klass2;

class Klass1 {
}
````

Violation (Enclosed package `x.y.z` tries to reference enclosing pacakge `x.y`):

````java
package x.y.z;

import x.y.Klass1;

class Klass2 {
}
````

Unrelated packages are free to import each other in any direction:

Ok (`x.v` and `x.w` are unrelated packages, so no rules are imposed):

````java
package x.v;

import x.w.Klass2;

class Klass1 {
}
````

Additionally there is a special treatment of the package named `internal`.
It is forbidden to import package `internal` unless the import is from the directly enclosing package.

Ok (Package `x.y.internal` is directly enclosed by `x.y`):

````java
package x.y;

import x.y.internal.Klass2;

class Klass1 {
}
````

Violation (Package `x.y.z.internal` is enclosed, but not directly by `x.y`):

````java
package x.y;

import x.y.z.internal.Klass2;

class Klass1 {
}
````

Violation (Package `x.w.internal` is unrelated to `x.v` package):

````java
package x.v;

import x.w.internal.Klass2;

class Klass1 {
}
````

Example snippet in `checkstyle.xml`:

````xml
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.NoImportsOfHigherPackagesCheck" />
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

### com.github.sviperll.grumpinessy.MembersOrderCheck ###

Checks the ordering of members inside a class/interface.
Numbers in the configuration determines the order.
Members with smaller number should be defined higher in the source code.

Example snippet in `checkstyle.xml`:

````xml
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.MembersOrderCheck">
            <property name="staticVariableOrdinal" value="1"/>
            <property name="staticInitializerOrdinal" value="2"/>
            <property name="staticMethodOrdinal" value="3"/>
            <property name="instanceVariableOrdinal" value="4"/>
            <property name="instanceInitializerOrdinal" value="5"/>
            <property name="constructorOrdinal" value="6"/>
            <property name="instanceMethodOrdinal" value="7"/>
            <property name="innerClassOrdinal" value="8"/>
            <property name="staticNetstedClassOrdinal" value="9"/>
        </module>
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

Numbers are not to be nessesary unique, same number means that members can be freely mixed.
The following snippet allows to freely mix static and non-static members:

````xml
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.MembersOrderCheck">
            <property name="staticVariableOrdinal" value="1"/>
            <property name="instanceVariableOrdinal" value="1"/>
            <property name="staticInitializerOrdinal" value="2"/>
            <property name="instanceInitializerOrdinal" value="2"/>
            <property name="constructorOrdinal" value="3"/>
            <property name="staticMethodOrdinal" value="4"/>
            <property name="instanceMethodOrdinal" value="4"/>
            <property name="innerClassOrdinal" value="5"/>
            <property name="staticNetstedClassOrdinal" value="5"/>
        </module>
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

### com.github.sviperll.grumpinessy.MethodCallLineBreaksCheck ###

Checks that method call either stays on a single line, or otherwise
each argument stays on a separate line with
opening and closing parenthesis also occupying a stand-alone line.

Ok (single line):

````java
    x.method1(z, y);
````

Ok (multiple lines):

````java
    x.method1(
            z,
            y
    );
````

Violation (multiple arguments on a single line):

````java
    x.method1(
            z, y
    );
````

Violation (closing parenthesis doesn't occupy a stand-alone line):

````java
    x.method1(
            z,
            y);
````

Example snippet in `checkstyle.xml`:

````
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.MethodCallLineBreaksCheck" />
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

### com.github.sviperll.grumpinessy.MethodCallChainLineBreaksCheck ###

Checks that a chain of method calls either stays on a single line, or otherwise
each call is on a separate line.

Ok (single line):

````java
    v.w().x(y).z();
````

Ok (multiple lines):

````java
    v.w()
            .x(y)
            .z();
````

Violation (multiple (but not all) calls on a single line):

````java
    v.w().x(y)
            .z();
````

Example snippet in `checkstyle.xml`:

````
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.MethodCallChainLineBreaksCheck" />
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

### com.github.sviperll.grumpinessy.IfElseSameBracesCheck ###

Checks that every if-statement either has curly braces around both then- and else-statements or
has no curly braces around both then- and else-statements.

Ok (both have no braces):

````java
    if (x)
        break;
    else
        continue;
````

Ok (both have braces):

````java
    if (x) {
        x.y();
    } else {
        return;
    }
````

Violation (mixed usage of braces):

````java
    if (x) {
        x.y();
    } else
        return;
````

The check also applies to else-if chains:

Ok (all have no braces):

````java
    if (x)
        break;
    else if (y)
        y.z();
    else
        continue;
````

Ok (all have braces):

````java
    if (x) {
        x.w();
    } else if (y) {
        y.z();
    } else {
        return;
    }
````

Example snippet in `checkstyle.xml`:

````
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.IfElseSameBracesCheck" />
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````

### com.github.sviperll.grumpinessy.NessesaryBracesCheck ###

Checks that statements enclosed into if/else-, for- and while-statments
should be enclosed into curly braces, when statements occupy multiple lines.

Ok (single-line, no braces):

````java
if (x)
    x.w(v, y);
````

Ok (multiple-lines, braces):

````java
if (x) {
    throw new IOException(
        "%s: read error".formatted(
            fileName
        )
    );
}
````

Ok (multiple-lines, braces):

````java
if (x) {
    for (int i = 0; i < n; i++)
        print(x);
}
````

Violation (multiple-lines, no braces):

````java
if (x)
    throw new IOException(
        "%s: read error".formatted(
            fileName
        )
    );
````

Violation (multiple-lines, no braces):

````java
if (x)
    for (int i = 0; i < n; i++)
        print(x);
````

Example snippet in `checkstyle.xml`:

````
<module name="Checker">
    <!-- ... -->
    <module name="TreeWalker">
        <!-- ... -->
        <module name="com.github.sviperll.grumpinessy.NessesaryBracesCheck" />
        <!-- ... -->
    </module>
    <!-- ... -->
</module>
````
