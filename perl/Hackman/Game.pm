package Hackman::Game;

use Moose::Role;
use namespace::autoclean;

use Hackman::Field;

$| = 1; # flush STDOUT after every print
my @Command;
sub _get_next_command {
    my $line = <STDIN>;
    chomp $line;

    @Command = split ' ', $line;
    #warn "COMMAND=$Command[0]";
}

# --------------------------------
# Settings

has timebank      => (is => 'ro', required => 1);
has time_per_move => (is => 'ro', required => 1);
has max_rounds    => (is => 'ro', required => 1);
has your_bot_name => (is => 'ro', required => 1);
has your_bot_id   => (is => 'ro', required => 1);
has field_height  => (is => 'ro', required => 1);
has field_width   => (is => 'ro', required => 1);

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    my $arg   = $class->$orig(@_);

    while (1) {
        _get_next_command();
        my ($command, $type, $value) = @Command;

        last if $command ne 'settings';

        if ($type eq 'your_bot') {
            $type = 'your_bot_name';
        } elsif ($type eq 'your_botid') {
            $type = 'your_bot_id';
        }
        $arg->{$type} = $value;
    }

    return $arg;
};

# --------------------------------
# Game state

has round  => (is => 'ro', writer => '_set_round');
has field  => (is => 'ro', writer => '_set_field');
has player => (is => 'ro', writer => '_set_player');
has enemy  => (is => 'ro', writer => '_set_enemy');
has time   => (is => 'ro', writer => '_set_time');

sub update_game_state {
    my ($self) = @_;

    my %player;
    my %enemy;

    _get_next_command() if $Command[0] eq 'action';
    while (1) {
        if ($Command[0] eq 'action') {
            my ($command, $type, $value) = @Command;
            $self->_set_time($value);
            last;
        }

        my ($command, $player_name, $type, $value) = @Command;

        if ($type eq 'round') {
            $self->_set_round($value);
        }
        elsif ($type eq 'field') {
            my $field = Hackman::Field->new(
                height => $self->field_height,
                width  => $self->field_width,
                cells  => $value,
            );
            $self->_set_field($field);
        }
        else {
            my $player = $player_name eq $self->your_bot_name ? \%player : \%enemy;

            if ($type eq 'snippets') {
                $player->{snippets} = $value;
            }
            elsif ($type eq 'has_weapon') {
                $player->{has_weapon} = $value eq 'true';
            }
            elsif ($type eq 'is_paralyzed') {
                $player->{is_paralyzed} = $value eq 'true';
            }
        }

        _get_next_command();
    }

    for my $player ($self->field->players) {
        if ($player->id == $self->your_bot_id) {
            $player->update(%player);
            $self->_set_player($player);
        } else {
            $player->update(%enemy);
            $self->_set_enemy($player);
        }
    }
    return;
}

1;
