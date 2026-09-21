#!/usr/bin/env perl
#
# Fails when a positional method of a collection lacks a "Complexity:" line in its javadoc (design.md 3.7).
#
#   perl scripts/check-complexity.pl zazr-core/src/main/java/com/guizmaii/zazr/collection/Vector.java ...
#
# How a javadoc block is matched to a method: each file is read top to bottom. A `/** ... */` block, or a run of
# `///` lines, is remembered as "the pending javadoc". Blank lines, annotations (`@Override`, `@SuppressWarnings`...)
# and `//` comments between it and the next code line are skipped. The next code line then either declares a method
# at class-member indentation (exactly four spaces, then modifiers or a return type, then `name(`), in which case the
# pending javadoc is that method's, or it is something else (a field, a nested class, a statement of a one-line
# method...), and the pending javadoc is dropped: a javadoc never attaches to a method further down. A method whose
# name is in the list below and whose javadoc (if any) has no line containing "Complexity:" is reported. Every
# overload is checked separately. Only names actually declared in the file at that indentation are checked, so a
# nested class's methods (indented deeper) and names the file does not declare are ignored.
#
use strict;
use warnings;

my @positional = qw(
    get update insert insertAll removeAt head tail init last slice subSequence take takeRight takeWhile takeUntil
    drop dropRight dropWhile dropUntil append appendAll prepend prependAll reverse sorted sortBy zip zipAll zipWith
    zipWithIndex sliding grouped scan scanLeft scanRight indexOf lastIndexOf indexWhere lastIndexWhere search padTo
    patch permutations combinations crossProduct intersperse rotateLeft rotateRight shuffle splitAt startsWith
    endsWith distinct distinctBy remove removeAll removeFirst removeLast replace replaceAll leftPadTo asJava
);
my %positional = map { $_ => 1 } @positional;

# a member declaration: exactly 4 spaces (the fifth column is not a space, so statements of method bodies, indented
# deeper, never match), optional modifiers, optional type parameters, a return type, the name, `(`
my $modifiers = qr/(?:(?:public|protected|private|static|final|abstract|default|synchronized|native)\s+)*/;
my $declaration = qr/^ {4}(?=\S)${modifiers}(?:<[^{;=]*?>\s+)?[\w.<>,?\[\]@ ]+?\s+(\w+)\s*\(/;

die "usage: $0 FILE...\n" unless @ARGV;

my $failures = 0;
my $checked = 0;
for my $file (@ARGV) {
    open(my $fh, '<', $file) or die "cannot read $file: $!\n";
    my $doc = '';       # the pending javadoc
    my $has_doc = 0;
    my $in_block = 0;   # inside a /** ... */ block
    my $markdown = 0;   # the pending javadoc is a run of /// lines
    my $line_no = 0;
    while (my $line = <$fh>) {
        $line_no++;
        if ($in_block) {
            $doc .= $line;
            if ($line =~ m{\*/}) { $in_block = 0; $has_doc = 1; }
            next;
        }
        if ($line =~ m{^\s*/\*\*}) {
            $doc = $line; $has_doc = 0; $markdown = 0;
            $in_block = ($line !~ m{\*/});
            $has_doc = 1 unless $in_block;
            next;
        }
        if ($line =~ m{^\s*///}) {
            if ($markdown && $has_doc) { $doc .= $line; } else { $doc = $line; $markdown = 1; $has_doc = 1; }
            next;
        }
        next if $line =~ m{^\s*$} || $line =~ m{^\s*@} || $line =~ m{^\s*//};
        if ($line =~ $declaration) {
            my $name = $1;
            if ($positional{$name}) {
                $checked++;
                unless ($has_doc && $doc =~ m{Complexity:}) {
                    print "$file:$line_no: $name() has no 'Complexity:' line in its javadoc\n";
                    $failures++;
                }
            }
        }
        $doc = ''; $has_doc = 0; $markdown = 0;
    }
    close($fh);
}
if ($checked == 0) {
    print "no positional method found in @ARGV\n";
    exit 1;
}
if ($failures) {
    print "$failures positional method(s) without a 'Complexity:' line (see docs/design.md 3.7)\n";
    exit 1;
}
print "complexity: $checked positional method declarations checked, all documented\n";
